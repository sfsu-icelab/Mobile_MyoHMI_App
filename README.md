# MyoHMI Android App
A mobile Android application used to implement SFSU ICE lab Myo Human Machine Interface Gesture Recognition Algorithms. The original code was developed by Alex D. and other graduate and undergraduate students in the lab.

This application is to be paired with either Thalmic Labs Myo Armband, a low cost emg/imu wearable sensor, or to a microcontroller with Bluetooth Low Energy (BLE) capabilities. The app connects to the device via BLE and reads EMG data in real time. EMG signals are subjected to time domain feature extraction and passed to Machine Learning algorithms with the help of the SMILE Java Machine Learning libraries. After proper training, the app can predict most hand gestures.

This version of the code adds the additional capability for the app to connect to a BLE-equipped microcontroller rather than only connecting to the Myo Armband as was done in previous versions. Unlike with the Myo Armband, a custom microcontroller can be used to sample data from any number of sensors (such as Myoware Muscle Sensors) at any frequency allowed by the BLE module's bandwidth. Therefore, the app shall accomodate this variability by allowing the user to select the number of channels to be sampled, and which of these channels shall be plotted.

UPDATES:
- layout-normal > fragment_emg.xml
- java > example.ASPIRE.MyoHMI_Android > EmgFragment.java
- - java > example.ASPIRE.MyoHMI_Android > Plotter.java
  - Added an extra layout which allows the user to plot up to 4 channels simultaneously.
  - Added drop-down menus for each channel, so that the user can plot whichever channel they choose to.
    - By default, channels 1-4 shall be plotted (1-n if n < 4)
    - If the user wishes to plot a different channel, they shall select that option from one of the 4 drop-down menus

- java > example.ASPIRE.MyoHMI_Android > ListActivity.java
  - Added a field for the user to enter the number of channels to be expected.
  - Originally had a single scan button and listview for searching for BLE devices.
    - The number of channels shall be an integer greater than 0

- java > example.ASPIRE.MyoHMI_Android > FeatureCalculator.java
  - Connection to Hackberry Arm shall no longer be required for feature extraction to work.

- java > example.ASPIRE.MyoHMI_Android > MyoGattCallback.java
  - Added additional logic in the "onCharacteristicChanged" method to handle microcontroller data retrieval separately.

- java > example.ASPIRE.MyoHMI_Android > Plotter.java
  - Added function to handle plotting of variable data.
