# -*- coding: utf-8 -*-
"""
Created on Tue Apr 13 18:22:21 2021

@author: Amir Modan

Program containing methods for retrieving data from dataset

"""

def dataset_mat_Myo(fileAddress, window_length):
    """
    Scans a .mat file for sEMG data
    .mat file taken from NinaPro Dataset DB5

    Parameters
    ----------
    fileAddress: String
        The location of the file to be read on the file explorer
        Must end with '.txt'
    rows: Integer
        Number of rows in the sEMG Array
    cols: Integer
        Number of columns in the sEMG Array
    

    Returns
    -------
    data: List (List (List (Integer)))
        List of sEMG images
    """
    
    import scipy.io
    
    # Read .mat file into Dictionary object and extract list of data
    mat = scipy.io.loadmat(fileAddress)['emg']
    
    labels = scipy.io.loadmat(fileAddress)['restimulus']
    
    # Instantiate empty dataset
    data = [[],[],[],[],[],[],[],[]]
    encoded_labels = []
    data_windows = [[[0 for channel in range(8)] for window_index in range(window_length)] for sample in range(8)]
    window_iter = [0,0,0,0,0,0,0,0]
    
    # Iterate through samples
    for row in range(len(mat)):    
        current_gesture = -1
        
        # See NinaPro Dataset for information on gestures performed
        if labels[row][0] == 0:
            # Set to Rest
            current_gesture = 0
            
        elif labels[row][0] == 6:
            # Set to Fist
            current_gesture = 1
            
        elif labels[row][0] == 7:
            # Set to Point
            current_gesture = 2
        
        elif labels[row][0] == 5:
            # Set to All Finger Abduction (Open Hand)
            current_gesture = 3
        
        elif labels[row][0] == 13:
            # Set to Wrist Flexion (Wave-In)
            current_gesture = 4
            
        
        elif labels[row][0] == 14:
            # Set to Wrist Extension (Wave-Out)
            current_gesture = 5
            
        
        elif labels[row][0] == 11:
            # Set to Wrist Supination
            current_gesture = 6
            
        
        elif labels[row][0] == 12:
            # Set to Wrist Pronation
            current_gesture = 7
        
        # If the current gesture being read is one that we want for training, save to that gesture's buffer
        if current_gesture >= 0:
            # Save to current buffer location
            data_windows[current_gesture][window_iter[current_gesture]] = mat[row][0:8]
            
            # Buffer is not full
            if window_iter[current_gesture] < 51:
                window_iter[current_gesture] = window_iter[current_gesture] + 1
                
            # Buffer is full, so save as sEMG image with an associated gesture label
            else:
                data[current_gesture].append(data_windows[current_gesture])
                data_windows = [[[0 for channel in range(8)] for window_index in range(window_length)] for sample in range(8)]
                encoded_label = [0,0,0,0,0,0,0,0]
                encoded_label[current_gesture] = 1
                encoded_labels.append(encoded_label)
                window_iter[current_gesture] = 0
    
    return data, encoded_labels


def dataset_mat_CSL(fileAddress, rows, cols):
    """
    Scans a .mat file for sEMG data

    Parameters
    ----------
    fileAddress: String
        The location of the file to be read on the file explorer
        Must end with '.txt'
    rows: Integer
        Number of rows in the sEMG Array
    cols: Integer
        Number of columns in the sEMG Array
    

    Returns
    -------
    data: List (List (List (Integer)))
        List of sEMG images
    """
    
    import scipy.io
    
    # Read .mat file into Dictionary object and extract list of data
    mat = scipy.io.loadmat(fileAddress)['gestures'][0][0]
    # Instantiate empty dataset
    data = [[[0 for col in range(cols)]for row in range(rows)] for samp in range(len(mat[0]))]
    #data = [[[0] * cols] * rows] * len(mat[0])
    
    # Iterate through samples
    for i in range(len(data)):
        # Iterate through rows for each sample
        for j in range(rows):
            # Iterate through columns for each row
            for k in range(cols):
                # Save current sample to dataset
                data[i][j][k] = mat[j*cols+k][i]
    return data

def dataset_mat_ICE(fileAddress, rows, cols):
    """
    Scans a .mat file for sEMG data

    Parameters
    ----------
    fileAddress: String
        The location of the file to be read on the file explorer
        Must end with '.txt'
    rows: Integer
        Number of rows in the sEMG Array
    cols: Integer
        Number of columns in the sEMG Array
    

    Returns
    -------
    data: List (List (List (Integer)))
        List of sEMG images
    """
    
    import scipy.io
    
    # Read .mat file into Dictionary object and extract list of data
    mat = scipy.io.loadmat(fileAddress)['Data']
    
    # Instantiate empty dataset
    #data = [[[0] * cols] * rows] * len(mat)
    data = [[[0 for col in range(cols)]for row in range(rows)] for samp in range(len(mat))]
    # Iterate through samples
    for i in range(len(mat)):
        # Iterate through rows for each sample
        for j in range(rows):
            # Iterate through columns for each row
            for k in range(cols):
                # Save current sample to dataset
                data[i][j][k] = mat[i][j*cols+k]
    
    return data
    

def dataset(fileAddress, window_length, sliding_increment=1):
    """
    Scans a .txt file for sEMG data

    Parameters
    ----------
    fileAddress: String
        The location of the file to be read on the file explorer
        Must end with '.txt'
    window_length: Integer
        Number of samples in each Window
    sliding_increment: Integer
        Number of time units the window should shift forward
        Defaults to 1

    Returns
    -------
    List (List (Integer))
        List of sEMG windows
    """
    #Opens a .txt file
    file = open(fileAddress,"r")
    #Splits lines into a String Array
    content = file.read().splitlines()
    #Closes the file
    file.close()
    data = [[0.0 for i in range(8)] for j in range(5000)]
    
    current_window = -100
    #Loops through each line and assigns to data array
    for str in content:
        #If line is empty, indicates current window is complete
        if(len(str) == 0):
            current_window = current_window + 100;
            continue
        #Indicates a new window of data
        elif(str[0:2] == '{{'):
            channel = 0
            string_arr = str.replace('{', '').replace('}', '').replace(',', '').split()
            for i in range(len(string_arr)):
                data[current_window+i][channel] = int(string_arr[i]) / 8
            channel = channel + 1
        #Indicates the next channel
        elif(str[0] == '{'):
            string_arr = str.replace('{', '').replace('}', '').replace(',', '').split()
            for i in range(len(string_arr)):
                data[current_window+i][channel] = int(string_arr[i]) / 8
            channel = channel + 1
    
    return [data[i:i + window_length] for i in range(0, len(data)-100, sliding_increment)]

def extractFeatures(windows):
    """
    Extracts features for each channel

    Parameters
    ----------
    windows: List (List (Integer))
        List of sEMG windows

    Returns
    -------
    features: List (List (float))
    """
    #Extract MAV for 8 sample window    
    features = [[0.0 for i in range(8)] for j in range(len(windows))]
    for i in range(len(windows)):
        numsum = [0 for samp in range(len(windows[i]))]
        for sample in windows[i]:
            for channel in range(len(sample)):
                numsum[channel] += abs(sample[channel])
        for channel in range(8):
            features[i][channel] = numsum[channel]/len(windows[i])
    return features

def extractFeaturesHD(data, rows, cols, window_length, sliding_increment=1):
    """
    Extracts features for each channel in an HD Image

    Parameters
    ----------
    data: List (List (List (Integer)))
        List of raw sEMG data samples for each channel in the image
    rows: Integer
        Number of rows in image
    cols: Integer
        Number of columns in image
    window_length: Integer
        Number of samples in each Window
    sliding_increment: Integer
        Number of time units the window should shift forward
        Defaults to 1

    Returns
    -------
    features: List (List (List (float)))
        List of extracted features for each channel in the image
    """
    
    # Determine length of data that can fit into complete windows
    data_length = len(data) - (len(data) % window_length)
    
    # Segment data into complete windows
    windows = [data[i:i + window_length] for i in range(0, data_length, sliding_increment)]
    
    # Initialize empty list of features
    features = [[[0.0 for i in range(cols)] for j in range(rows)] for k in range(len(windows))]

    # Loop through windows
    for window in range(len(windows)):
        
        # Initialize sum for a window
        numsum = [[0 for samp in range(cols)] for row in range(rows)]
        
        # Loop through samples in a window
        for sample in range(window_length):
            # Loop through rows
            for row in range(rows):
                # Loop through columns
                for col in range(cols):
                    # Add reading to corresponding sum for a single channel
                    numsum[row][col] += abs(windows[window][sample][row][col])
        
        # Extract MAV from sums for each window
        for row in range(rows):
            for col in range(cols):
                features[window][row][col] = numsum[row][col]/len(windows[window])
                
    return features