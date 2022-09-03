package example.ASPIRE.MyoHMI_Android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    public static final int MENU_LIST = 0;
    public static final int MENU_BYE = 1;

    private static final long SCAN_PERIOD = 5000;

    private static final int REQUEST_ENABLE_BT = 1;

    private static final String TAG = "BLE_Myo";
    private static final String FORMAT = "%2d";
    private static Plotter plotter = new Plotter();//static may cause issues
    public int gestureCounter = 0;
    public ClassificationFragment classificationFragment;
    TextView countdown;

    //Added by Alex L.
    //Needed to Receive Vibration Requests from Unity.
    private ReceiveFromUnity receiveFromUnity;

    /***********************Below ADDED BY CHARLES FOR SWIPEABLE TABS***************************/

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    TabLayout tabLayout;
    private ScanCallback scanCallback = new ScanCallback() {
    };
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private TextView emgDataText;
    private TextView gestureText;
    private BluetoothLeScanner mLEScanner;
    private MyoGattCallback mMyoCallback;
    private MyoCommandList commandList = new MyoCommandList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager) this.findViewById(R.id.view_pager);
        mViewPager.setOffscreenPageLimit(3);
//
        EmgFragment emgFragment = new EmgFragment();
        FeatureFragment featureFragment = new FeatureFragment();
        classificationFragment = new ClassificationFragment();
        ImuFragment imuFragment = new ImuFragment();

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        TabLayout.Tab EMGTab = tabLayout.newTab();
        TabLayout.Tab ArmbandTab = tabLayout.newTab();
        TabLayout.Tab FeatureTab = tabLayout.newTab();
        TabLayout.Tab IMUTab = tabLayout.newTab();
        TabLayout.Tab ClassificationTab = tabLayout.newTab();

        tabLayout.addTab(EMGTab, 0, true);
        tabLayout.addTab(ArmbandTab, 1, true);
        tabLayout.addTab(FeatureTab, 2, true);
        tabLayout.addTab(IMUTab, 3, true);
        tabLayout.addTab(ClassificationTab, 4, true);

        tabLayout.setupWithViewPager(mViewPager);

        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

//        CognitoCachingCredentialsProvider cognitoProvider = new CognitoCachingCredentialsProvider(
//                getApplicationContext(),
//                "us-west-2:916f7fdd-9429-4d93-8606-b46efd049d9b", // Identity pool ID
//                Regions.US_WEST_2 // Region
//        );
//
//        // Create LambdaInvokerFactory, to be used to instantiate the Lambda proxy.
//        LambdaInvokerFactory factory = new LambdaInvokerFactory(this.getApplicationContext(), Regions.US_WEST_2, cognitoProvider);
//
//        Lambda mLambda = new Lambda(cognitoProvider, factory);//pass context to static variables for use in feature calculator


        //To Send and Receive from Unity - Alex L.
        startService(new Intent(this, SendToUnity.class));
        receiveFromUnity = new ReceiveFromUnity();
        registerReceiver(receiveFromUnity, new IntentFilter("com.test.sendintent.IntentFromUnity"));

    }

    /***********************TOP ADDED BY CHARLES FOR SWIPEABLE TABS***************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.connect:
            case MENU_LIST:
                Intent intent = new Intent(this, ListActivity.class);
                startActivity(intent);
                return true;

            case R.id.disconnect:
                Intent mStartActivity = new Intent(MainActivity.this, MainActivity.class);
                int mPendingIntentId = 12;
                PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, mPendingIntentId, mStartActivity,
                        PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) MainActivity.this.getSystemService(getApplicationContext().ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
                //closeBLEGatt();
                Toast.makeText(getApplicationContext(), "Close GATT", Toast.LENGTH_SHORT).show();
                return true;
        }
        return false;
    }

    public void closeBLEGatt() {
        if (mBluetoothGatt == null) {
            return;
        }
        mMyoCallback.stopCallback();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);
            mLEScanner.startScan(scanCallback);
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            classificationFragment.givePath(data.getData(), this);
        }
    }

    public static class TabsAdapter extends FragmentStatePagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

        private final MainActivity mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
        private final ArrayList<Fragment> mFrag = new ArrayList<Fragment>();

        public TabsAdapter(MainActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = activity.getSupportActionBar();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Object tag = tab.getTag();
            for (int i = 0; i < mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    Log.d("Tab", String.valueOf(i));
                    plotter.setCurrentTab(i);
                    mViewPager.setCurrentItem(i);
                }
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            //set booleans to kill unseen processes
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            //set booleans to kill unseen processes
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args, Fragment frag) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mFrag.add(frag);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return mFrag.get(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
            /*
            MenuInflater inflater = mContext.getSupportMenuInflater();
            mContext.menu.clear();
            inflater.inflate(R.menu.MainActivity, mContext.menu);
            */
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }
    }
}


