package example.ASPIRE.MyoHMI_Android;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet;

import java.util.ArrayList;

/**
 *
 * Activity which plots raw sEMG data and features
 *
 * Created by Alex on 6/30/2017.
 *
 * Updated by Amir Modan (amir5modan@gmail.com)
 * Changes include:
 * <ui>
 *  <li>Adding Plotter for generic EMG devices</li>
 *  <li>Said plotter can plot any number of channels</li>
 *  <li>Which channel is plotted depends on drop-down selection</li>
 * </ui>
 */

public class Plotter extends Activity {
    //boolean emg;
    private static RadarChart mChart;
    private static Handler mHandler;
    private static int currentTab = 0; //current tab from MainActivity
    private static int nowGraphIndexIMU = 0;
    private static boolean[] featuresSelected = new boolean[]{true, true, true, true, true, true};
    public boolean startup = true;
    protected Typeface mTfLight;
    int[][][] dataList = new int[20][10][50];
    int[][] dataList1_a = new int[10][50];
    int[][] dataList1_b = new int[10][50];
    private LineChart cubicChart;
    private LineGraph lineGraph;
    private int lineColor = Color.rgb(64, 64, 64);
    private int nowGraphIndex = 3;
    private ArrayList<Number> f0, f1, f2, f3, f4, f5;
    private int w, x, y, z;
    private double pitch, roll, yaw;
    private int numChannels = ListActivity.getNumChannels();;

    public Plotter() {
    }

    public Plotter(RadarChart chart) {

        mChart = chart;
        mHandler = new Handler();

        mChart.setNoDataText("");
        mChart.setBackgroundColor(Color.TRANSPARENT);
        mChart.getDescription().setEnabled(false);
        mChart.setWebLineWidth(1f);
        mChart.setWebColor(Color.LTGRAY);
        mChart.setWebLineWidthInner(1f);
        mChart.setWebColorInner(Color.LTGRAY);
        mChart.setWebAlpha(100);
//      mChart.getLegend().setTextSize(20f);
        mChart.getLegend().setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);

        XAxis xAxis = mChart.getXAxis();
        //xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            private String[] mActivities = new String[]{"1", "2", "3", "4", "5", "6", "7", "8"};

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mActivities[(int) value % mActivities.length];
            }
        });
        YAxis yAxis = mChart.getYAxis();
        //yAxis.setTypeface(mTfLight);
        yAxis.setLabelCount(8, false);
        yAxis.setTextSize(9f);
        yAxis.setAxisMinimum(0);
        yAxis.setAxisMaximum(128);
        yAxis.setDrawLabels(false);

        twoDimArray featemg = new twoDimArray();
        featemg.createMatrix(6, 8);

        this.setCurrentTab(1);

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 6; j++) {
                featemg.setMatrixValue(j, i, 128);
            }
        }

        this.pushFeaturePlotter(featemg);

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 6; j++) {
                featemg.setMatrixValue(j, i, 0);
            }
        }

        this.pushFeaturePlotter(featemg);

//        this.setCurrentTab(0);
    }

    public Plotter(Handler handler, LineGraph line) {
        mHandler = handler;
        lineGraph = line;
    }

    public Plotter(Handler handler, LineChart cubicLine) {

        mHandler = handler;
        cubicChart = cubicLine;

        cubicChart.setViewPortOffsets(0, 0, 0, 0);
        cubicChart.setBackgroundColor(Color.rgb(51, 0, 100));
        // no description text
        cubicChart.getDescription().setEnabled(false);

        // enable touch gestures
        cubicChart.setTouchEnabled(true);

        // enable scaling and dragging
        cubicChart.setDragEnabled(true);
        cubicChart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        cubicChart.setPinchZoom(true);

        cubicChart.setDrawGridBackground(false);
        //cubicChart.setVisibleXRangeMinimum(0);
        //cubicChart.setVisibleYRangeMinimum();
        cubicChart.setMaxHighlightDistance(300);

        XAxis x = cubicChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        x.setTextSize(12f);
        x.setLabelCount(7, true);// force 10 labels
        x.setTextColor(Color.WHITE);
        x.setDrawAxisLine(true);
        x.setDrawGridLines(false);
        x.setAxisMinimum(0f);
        x.setEnabled(true);

        YAxis y = cubicChart.getAxisLeft();
        y.setTypeface(mTfLight);
        y.setLabelCount(10, true);// force 10 labels
        y.setTextColor(Color.WHITE);
        y.setTextSize(12f);// set the text size
        y.setAxisMinimum(0f);// start at zero
        //y.setAxisMaximum(100f);// the axis maximum is 100
        y.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        y.setDrawGridLines(true);
        y.setGranularity(1f); // interval 1
        y.setAxisLineColor(Color.WHITE);

        cubicChart.getAxisRight().setEnabled(false);

        // add data
        setData(4, 5);

        cubicChart.getLegend().setEnabled(true);


        cubicChart.animateXY(2000, 2000);

        // dont forget to refresh the drawing
        cubicChart.invalidate();

    }

    public static void setIMU(int imu) {
        nowGraphIndexIMU = imu;
    }

    private void setData(int count, float range) {

        ArrayList<Entry> xVals = new ArrayList<Entry>();
        ArrayList<Entry> yVals = new ArrayList<Entry>();
        ArrayList<Entry> zVals = new ArrayList<Entry>();

/*        for (int i = 0; i < count; i++) {
            float mult = range / 2f;
            float val = (float) (Math.random() * mult) + 50;
            xVals.add(new Entry(i, val));
        }


        for (int i = 0; i < count-1; i++) {
            float mult = range;
            float val = (float) (Math.random() * mult) + 450;
            yVals.add(new Entry(i, val));
//            if(i == 10) {
//                yVals2.add(new Entry(i, val + 50));
//            }
        }


        for (int i = 0; i < count; i++) {
            float mult = range;
            float val = (float) (Math.random() * mult) + 500;
            zVals.add(new Entry(i, val));
        }*/


        for (int i = 0; i < count; i++) {
            float mult = (range + 1);
            float val = (float) (Math.random() * mult) + 20;// + (float)
            // ((mult *
            // 0.1) / 10);
            xVals.add(new Entry(i, val));
            yVals.add(new Entry(i, val + 10));
            zVals.add(new Entry(i + 2, val + 20));
        }


        LineDataSet x, y, z;

        if (cubicChart.getData() != null &&
                cubicChart.getData().getDataSetCount() > 0) {
            x = (LineDataSet) cubicChart.getData().getDataSetByIndex(0);
            y = (LineDataSet) cubicChart.getData().getDataSetByIndex(1);
            z = (LineDataSet) cubicChart.getData().getDataSetByIndex(2);

            x.setValues(xVals);
            y.setValues(yVals);
            z.setValues(zVals);
            cubicChart.getData().notifyDataChanged();
            cubicChart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            x = new LineDataSet(yVals, "x");

            x.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            x.setCubicIntensity(0.2f);
            x.setDrawFilled(false);
            x.setDrawCircles(true); //check for numbers
            x.setLineWidth(1.8f);
            x.setCircleRadius(4f);
            x.setCircleColor(Color.WHITE);
            x.setHighLightColor(Color.rgb(244, 250, 117));
            x.setColor(Color.WHITE);
            x.setFillColor(Color.WHITE);
            x.setFillAlpha(40);
            x.setDrawHorizontalHighlightIndicator(true);
            x.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return 0;
                }
            });

            y = new LineDataSet(yVals, "y");

            y.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            y.setCubicIntensity(0.2f);
            y.setDrawFilled(true);
            y.setDrawCircles(true);
            y.setLineWidth(1.8f);
            y.setCircleRadius(4f);
            y.setCircleColor(Color.WHITE);
            y.setHighLightColor(Color.rgb(244, 117, 117));
            y.setColor(Color.WHITE);
            y.setFillColor(Color.BLACK);
            y.setFillAlpha(80);
            y.setDrawHorizontalHighlightIndicator(true);
            y.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return 0;
                }
            });

            z = new LineDataSet(yVals, "z");

            z.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            z.setCubicIntensity(0.2f);
            z.setDrawFilled(true);
            z.setDrawCircles(true);
            z.setLineWidth(1.8f);
            z.setCircleRadius(4f);
            z.setCircleColor(Color.WHITE);
            z.setHighLightColor(Color.rgb(244, 117, 117));
            z.setColor(Color.WHITE);
            z.setFillColor(Color.BLACK);
            z.setFillAlpha(80);
            z.setDrawHorizontalHighlightIndicator(true);
            z.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return 0;
                }
            });


            // create a data object with the datasets
            //LineData data = new LineData(x,y,z);
            LineData data = new LineData(x, y, z);

            data.setValueTypeface(mTfLight);
            data.setValueTextSize(12f);
            data.setDrawValues(true);
            data.setValueTextColor(Color.WHITE);

            // set data
            cubicChart.setData(data);
            //cubicChart.invalidate();
        }
    }

    public void pushPlotter(byte[] data) {
//        setData();
        if (data.length == 16 && (currentTab == 0 || currentTab == 1)) {
//        if ((data.length == 16 && currentTab == 0)||startup) {

//            Log.d("tag", String.valueOf(startup));

            mHandler.post(new Runnable() {
                @Override
                public void run() {
//                    dataView.setText(callback_msg);
//                    Log.d("In: ", "EMG Graph");
                    lineGraph.removeAllLines();

                    for (int inputIndex = 0; inputIndex < 8; inputIndex++) {
                        dataList1_a[inputIndex][0] = data[0 + inputIndex];
                        dataList1_b[inputIndex][0] = data[7 + inputIndex];
                    }
                    // 折れ線グラフ
                    int number = 50;
                    int addNumber = 100;
                    Line line = new Line();
                    while (0 < number) {
                        number--;
                        addNumber--;

                        //１点目add
                        if (number != 0) {
                            for (int setDatalistIndex = 0; setDatalistIndex < 8; setDatalistIndex++) {
                                dataList1_a[setDatalistIndex][number] = dataList1_a[setDatalistIndex][number - 1];
                            }
                        }
                        LinePoint linePoint = new LinePoint();
                        linePoint.setY(dataList1_a[nowGraphIndex][number]); //ランダムで生成した値をSet
                        linePoint.setX(addNumber); //x軸を１ずつずらしてSet
                        //linePoint.setColor(Color.parseColor("#9acd32")); // 丸の色をSet

                        line.addPoint(linePoint);
                        //2点目add
                        /////number--;
                        addNumber--;
                        if (number != 0) {
                            for (int setDatalistIndex = 0; setDatalistIndex < 8; setDatalistIndex++) {
                                dataList1_b[setDatalistIndex][number] = dataList1_b[setDatalistIndex][number - 1];
                            }
                        }
                        linePoint = new LinePoint();
                        linePoint.setY(dataList1_b[nowGraphIndex][number]); //ランダムで生成した値をSet
                        linePoint.setX(addNumber); //x軸を１ずつずらしてSet
                        //linePoint.setColor(Color.parseColor("#9acd32")); // 丸の色をSet

                        line.addPoint(linePoint);
                    }

                    line.setColor(lineColor); // 線の色をSet

                    line.setShowingPoints(false);
                    lineGraph.addLine(line);
                    lineGraph.setRangeY(-128, 128); // 表示するY軸の最低値・最高値 今回は0から1まで

                }
            });
        } else if (data.length == 20 && currentTab == 1) {//emg=false;
            w = data[0];
            x = data[1];
            y = data[2];
            z = data[3];

            roll = Math.atan(2 * (x * w + z * y) / (2 * (x ^ 2 + y ^ 2) - 1));
        }
    }

    /**
     * Plotter for generic ENG device
     * @param data An array of bytes of any length
     */
    public void pushMyowarePlotter(byte[] data) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                lineGraph.removeAllLines();

                for(int sample = 0; sample < data.length/8; sample++) {
                    for (int inputIndex = 0; inputIndex < 8; inputIndex++) {
                        dataList[sample][inputIndex][0] = data[inputIndex + (sample*8)];
                    }
                }


                int number = 50;
                int addNumber = 50*data.length;
                Line line = new Line();
                while (0 < number) {
                    number--;
                    for(int sample = 0; sample < data.length/8; sample++) {
                        addNumber--;

                        if (number != 0) {
                            for (int setDatalistIndex = 0; setDatalistIndex < 8; setDatalistIndex++) {
                                dataList[sample][setDatalistIndex][number] = dataList[sample][setDatalistIndex][number - 1];
                            }
                        }
                        LinePoint linePoint = new LinePoint();
                        linePoint.setY(dataList[sample][nowGraphIndex][number]);
                        linePoint.setX(addNumber);

                        line.addPoint(linePoint);
                    }

                }

                line.setColor(lineColor);

                line.setShowingPoints(false);
                lineGraph.addLine(line);
                lineGraph.setRangeY(-128, 128); // 表示するY軸の最低値・最高値 今回は0から1まで

            }
        });
    }

    public void pushFeaturePlotter(twoDimArray featureData) {
        if (mChart != null && currentTab == 1) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    f0 = featureData.getInnerArray(0);
                    f1 = featureData.getInnerArray(1);
                    f2 = featureData.getInnerArray(2);
                    f3 = featureData.getInnerArray(3);
                    f4 = featureData.getInnerArray(4);
                    f5 = featureData.getInnerArray(5);

                    ArrayList<RadarEntry> entries0 = new ArrayList<>();
                    ArrayList<RadarEntry> entries1 = new ArrayList<RadarEntry>();
                    ArrayList<RadarEntry> entries2 = new ArrayList<RadarEntry>();
                    ArrayList<RadarEntry> entries3 = new ArrayList<RadarEntry>();
                    ArrayList<RadarEntry> entries4 = new ArrayList<RadarEntry>();
                    ArrayList<RadarEntry> entries5 = new ArrayList<RadarEntry>();

                    for (int i = 0; i < numChannels; i++) {
                        //2000 per division 14 000 in total
                        if(f0.size() == numChannels && f1.size() == numChannels && f2.size() == numChannels && f3.size() == numChannels && f4.size() == numChannels && f5.size() == numChannels) {
                            entries0.add(new RadarEntry(setMaxValue(f0.get(i).floatValue() * 200)));
                            entries1.add(new RadarEntry(setMaxValue(f1.get(i).floatValue() * 200)));
                            entries2.add(new RadarEntry(setMaxValue(f2.get(i).floatValue() * 200)));
                            entries3.add(new RadarEntry(setMaxValue(f3.get(i).floatValue() * 170)));
                            entries4.add(new RadarEntry(setMaxValue(f4.get(i).floatValue() * 200)));
                            entries5.add(new RadarEntry(setMaxValue(f5.get(i).floatValue() * 200)));
                        }
                    }

                    ArrayList<IRadarDataSet> sets = new ArrayList<IRadarDataSet>();

                    RadarDataSet set0 = new RadarDataSet(entries0, "MAV");
                    set0.setColor(Color.rgb(123, 174, 157));
                    set0.setFillColor(Color.rgb(78, 118, 118));
                    set0.setDrawFilled(true);
                    set0.setFillAlpha(180);
                    set0.setLineWidth(2f);

                    RadarDataSet set1 = new RadarDataSet(entries1, "WAV");
                    set1.setColor(Color.rgb(241, 148, 138));
                    set1.setFillColor(Color.rgb(205, 97, 85));
                    set1.setDrawFilled(true);
                    set1.setFillAlpha(180);
                    set1.setLineWidth(2f);

                    RadarDataSet set2 = new RadarDataSet(entries2, "Turns");
                    set2.setColor(Color.rgb(175, 122, 197));
                    set2.setFillColor(Color.rgb(165, 105, 189));
                    set2.setDrawFilled(true);
                    set2.setFillAlpha(180);
                    set2.setLineWidth(2f);

                    RadarDataSet set3 = new RadarDataSet(entries3, "Zeros");
                    set3.setColor(Color.rgb(125, 206, 160));
                    set3.setFillColor(Color.rgb(171, 235, 198));
                    set3.setDrawFilled(true);
                    set3.setFillAlpha(180);
                    set3.setLineWidth(2f);

                    RadarDataSet set4 = new RadarDataSet(entries4, "SMAV");
                    set4.setColor(Color.rgb(39, 55, 70));
                    set4.setFillColor(Color.rgb(93, 109, 126));
                    set4.setDrawFilled(true);
                    set4.setFillAlpha(180);
                    set4.setLineWidth(2f);

                    RadarDataSet set5 = new RadarDataSet(entries5, "AdjUnique");
                    set5.setColor(Color.rgb(10, 100, 126)); // 100 50 70
                    set5.setFillColor(Color.rgb(64, 154, 180));
                    set5.setDrawFilled(true);
                    set5.setFillAlpha(180);
                    set5.setLineWidth(2f);

                    if (featuresSelected[0])
                        sets.add(set0);
                    if (featuresSelected[1])
                        sets.add(set1);
                    if (featuresSelected[2])
                        sets.add(set2);
                    if (featuresSelected[3])
                        sets.add(set3);
                    if (featuresSelected[4])
                        sets.add(set4);
                    if (featuresSelected[5])
                        sets.add(set5);

                    if (!sets.isEmpty()) {
                        RadarData data = new RadarData(sets);
                        data.setValueTextSize(18f);
                        data.setDrawValues(false);
                        mChart.setData(data);
                        mChart.notifyDataSetChanged();
                        mChart.invalidate();
                    } else {
                        mChart.clear();
                    }
                }
            });
        } else if (mChart == null) {
            Log.d("wassup ", "mchart might be null************************************");
        }
    }

    public void setEMG(int color, int emg) {
        lineColor = color;
        nowGraphIndex = emg;
    }

    public void setCurrentTab(int tab) {
        currentTab = tab;
    }

    public void setFeatures(boolean[] features) {
        featuresSelected = features;
    }

    public float setMaxValue(float inValue) {
        float value = inValue;
        if (inValue > 14000) {
            value = 14000;
        }
        return value;
    }
}
