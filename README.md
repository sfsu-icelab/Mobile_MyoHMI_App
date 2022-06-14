# MyoHMI Android App
A mobile Android application used to implement SFSU ICE lab Myo Human Machine Interface Gesture Recognition Algorithms. The original code was developed by Alex D. and other graduate and undergraduate students in the lab. The previous student who worked on this app was Alex Louie. The most updated version of the app and the code that I worked on was pulled from his Github, which can be found here: https://github.com/louiealex/Myo-HMI-Android.

This application is to be paired with Thalmic Labs Myo Armband, a low cost emg/imu wearable sensor. The app connects to the armband via BLE and reads EMG data in real time. EMG signals are subjected to time domain feature extraction and passed to Machine Learning algorithms with the help of the SMILE Java Machine Learning libraries. After proper training, the app can predict most hand gestures.

This version of the code is being developed to enable an additional Bluetooth connection to an ESP32 microcontroller, which is controlling a HACKberry Arm. The goal is to maintain 2 connections with a BLE device (Myo Armband) and a Bluetooth device (Hackberry Arm) and use the predictions from the Machine Learning model to control the HACKberry Arm. The arm should be able to mimic the user's hand gestures in real-time.

UPDATES:
- layout-normal > activity_list.xml (Modified)
- java > example.ASPIRE.MyoHMI_Android > ListActivity.java (Modified)
  - Changed this layout file and its related Java file
  - Originally had a single scan button and listview for searching for BLE devices. 
    - Scan for BLE devices starts immediately after you press the button in menu > menu_main.xml (White MyoArmband icon). 
    - Scan lasts 5s and you can restart the scan by pressing the "SCAN" button. 
    - Once you select one of the listed BLE devices found, the connection starts.
  - Now has 2 scan buttons and 2 listviews for finding/scanning for BLE and Bluetooth devices separately. There is a separate "CONNECT" button.
    - Can now scan for BLE and Bluetooth devices separately. Scan lasts for 5s and results are stored in 2 separate listviews.
    - Scans for BLE devices and Bluetooth devices must be conducted separately.
    - You can choose to connect to either 1 MyoArmband, in order to perform the same function as the previous version of code, or choose to connect to a MyoArmband (BLE) and a listed Bluetooth device.
    - The selected devices are highlighted in a light-grey color.    
    - The HACKberry Arm is connected to an ESP32 microcontroller with a built-in Bluetooth module.
    - The ESP32 is programmed to receive Bluetooth commands in the form of serial data.
      - Ex: "P50 T100 O60" or "P90 T75" or "P120"  
    - You get stopped if you don't select any BLE device and prompted to try again.
  - Once you've made your selection, you can start the connections by pressing the "CONNECT" button.
    - The MainActivity is started again, but the name of your selected BLE device and the MAC address of your selected Bluetooth device are sent over in order to start the connections

- java > example.ASPIRE.MyoHMI_Android > BluetoothConnection.java (Added)
  - Added a Java class to handle Bluetooth operations (start connection/send data)

- java > example.ASPIRE.MyoHMI_Android > FeatureCalculator.java (Modified)
  - Added an instance of the BluetoothConnection.java class
  - Added a getMostFrequent() function to find the most frequent integer in an array
  - Modified the pushClassifier() function
    - Stores 10 predictions made by the trained Machine Learning (ML) classifier and sends the command for the most predicted hand gesture to the HACKberry Arm
    - Commands are sent through the instance of the BluetoothConnection.java class

- layout-normal > fragment_classification.xml (Modified)
  - Changed this layout file and its related Java file
  - Added an EditText field under the original one for adding gestures to the gesture list
    - The id of the original EditText is "add_gesture_text" and has a hint text "Add Gesture" 
    - The id of the new EditText is "add_command_text" and has a hint text "Add Command"
    - Added a small margin between the add gesture button ("im_add") and the EditText fields
  - Allows you to add an associated command to your added gesture that you can send to the HACKberry Arm once the trained ML model predicts your movement
  - If you don't enter an associated command, the default "Rest" command will be sent ("P90 T100 O140")

- java > example.ASPIRE.MyoHMI_Android > ClassificationFragment.java (Modified) (Needs final testing)
  - Added an ArrayList named "selectedCommands" to store commands that are associated with the selected gestures in the gesture list
  - Added a List that stores the default commands associated to the default selected gestures
  - Added code to make sure the commands in the "selectedCommands" ArrayList and the "selectedItems" ArrayList match
  - In onCreateView(), a Bluetooth connection is established if you selected one


