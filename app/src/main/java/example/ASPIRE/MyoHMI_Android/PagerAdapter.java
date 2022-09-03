package example.ASPIRE.MyoHMI_Android;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs = 5;
    private String[] tabTitles = new String[]{"EMG", "Myo Armband", "FEATURES", "IMU", "CLASSIFIER"};

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                EmgFragment tab1 = new EmgFragment();
                return tab1;
            case 1:
                ArmbandEmgFragment tab2 = new ArmbandEmgFragment();
                return tab2;
            case 2:
                FeatureFragment tab3 = new FeatureFragment();
                return tab3;
            case 3:
                ImuFragment tab4 = new ImuFragment();
                return tab4;
            case 4:
                ClassificationFragment tab5 = new ClassificationFragment();
                return tab5;
            default:
                return null;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
