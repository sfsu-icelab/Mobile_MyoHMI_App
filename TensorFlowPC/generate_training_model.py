# Copyright 2021 The TensorFlow Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""CLI wrapper for tflite_transfer_converter.

Converts a TF model to a TFLite transfer learning model.
"""

import os

import numpy as np
import tensorflow as tf
from DataRetrieval import dataset_mat_Myo

IMG_SIZE = 32
ROWS = 52
COLS = 8
NUM_FEATURES = 8
NUM_CLASSES = 8


class TransferLearningModel(tf.Module):
  """TF Transfer Learning model class."""

  def __init__(self, learning_rate=0.001):
    """Initializes a transfer learning model instance.

    Args:
      learning_rate: A learning rate for the optimzer.
    """
    self.num_features = NUM_FEATURES
    self.num_classes = NUM_CLASSES

    # trainable weights and bias for softmax
    self.ws = tf.Variable(
        tf.zeros((self.num_features, self.num_classes)),
        name='ws',
        trainable=True)
    self.bs = tf.Variable(
        tf.zeros((1, self.num_classes)), name='bs', trainable=True)

    # base model
    # self.base = tf.keras.applications.MobileNetV2(
    #     input_shape=(IMG_SIZE, IMG_SIZE, 3),
    #     alpha=1.0,
    #     include_top=False,
    #     weights='imagenet')
    
    CNN1 = tf.keras.layers.Conv2D(
            filters=32,
            strides=1,
            kernel_size=(5, 3), # 3x5 window
            activation='relu',
            input_shape=(ROWS, COLS, 1)
        )
    CNN2 = tf.keras.layers.Conv2D(
        filters=64,
        strides=1,
        kernel_size=(5,3), # 3x5 window
        activation='relu'
    )

    self.base = tf.keras.Sequential([
        # """
        # First CNN Feature Extraction Block
        # """
        CNN1,
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.PReLU(),
        tf.keras.layers.SpatialDropout2D(rate=0.5),
        tf.keras.layers.MaxPool2D(pool_size=(3,1)),
        # """
        # Second CNN Feature Extraction Block
        # """
        CNN2,
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.PReLU(),
        tf.keras.layers.SpatialDropout2D(rate=0.5),
        tf.keras.layers.MaxPool2D(pool_size=(3,1)),
        
        tf.keras.layers.Flatten(),
        
        tf.keras.layers.Dense(NUM_CLASSES),
        tf.keras.layers.Softmax(axis=-1)
    ])
    
    # loss function and optimizer
    self.loss_fn = tf.keras.losses.CategoricalCrossentropy()
    self.optimizer = tf.keras.optimizers.Adam(learning_rate=learning_rate)

  @tf.function(input_signature=[
      tf.TensorSpec([None, ROWS, COLS], tf.float32),
  ])
  def load(self, feature):
    """Generates and loads bottleneck features from the given image batch.

    Args:
      feature: A tensor of image feature batch to generate the bottleneck from.

    Returns:
      Map of the bottleneck.
    """
    x = tf.keras.applications.mobilenet_v2.preprocess_input(
        tf.multiply(feature, 255))
    bottleneck = tf.reshape(
        self.base(x, training=False), (-1, self.num_features))
    return {'bottleneck': bottleneck}

  @tf.function(input_signature=[
      tf.TensorSpec([None, NUM_FEATURES], tf.float32),
      tf.TensorSpec([None, NUM_CLASSES], tf.float32),
  ])
  def train(self, bottleneck, label):
    """Runs one training step with the given bottleneck features and labels.

    Args:
      bottleneck: A tensor of bottleneck features generated from the base model.
      label: A tensor of class labels for the given batch.

    Returns:
      Map of the training loss.
    """
    with tf.GradientTape() as tape:
      logits = tf.matmul(bottleneck, self.ws) + self.bs
      prediction = tf.nn.softmax(logits)
      loss = self.loss_fn(prediction, label)
    gradients = tape.gradient(loss, [self.ws, self.bs])
    self.optimizer.apply_gradients(zip(gradients, [self.ws, self.bs]))
    result = {'loss': loss}
    for grad in gradients:
      result[grad.name] = grad
    return result

  @tf.function(input_signature=[
      tf.TensorSpec([None, ROWS, COLS], tf.float32)
  ])
  def infer(self, feature):
    """Invokes an inference on the given feature.

    Args:
      feature: A tensor of image feature batch to invoke an inference on.

    Returns:
      Map of the softmax output.
    """
    x = tf.keras.applications.mobilenet_v2.preprocess_input(
        tf.multiply(feature, 255))
    bottleneck = tf.reshape(
        self.base(x, training=False), (-1, self.num_features))
    logits = tf.matmul(bottleneck, self.ws) + self.bs
    return {'output': tf.nn.softmax(logits)}

  @tf.function(input_signature=[tf.TensorSpec(shape=[], dtype=tf.string)])
  def save(self, checkpoint_path):
    """Saves the trainable weights to the given checkpoint file.

    Args:
      checkpoint_path: A file path to save the model.

    Returns:
      Map of the checkpoint file path.
    """
    tensor_names = [self.ws.name, self.bs.name]
    tensors_to_save = [self.ws.read_value(), self.bs.read_value()]
    tf.raw_ops.Save(
        filename=checkpoint_path,
        tensor_names=tensor_names,
        data=tensors_to_save,
        name='save')
    return {'checkpoint_path': checkpoint_path}

  @tf.function(input_signature=[tf.TensorSpec(shape=[], dtype=tf.string)])
  def restore(self, checkpoint_path):
    """Restores the serialized trainable weights from the given checkpoint file.

    Args:
      checkpoint_path: A path to a saved checkpoint file.

    Returns:
      Map of restored weight and bias.
    """
    restored_tensors = {}
    restored = tf.raw_ops.Restore(
        file_pattern=checkpoint_path,
        tensor_name=self.ws.name,
        dt=np.float32,
        name='restore')
    self.ws.assign(restored)
    restored_tensors['ws'] = restored
    restored = tf.raw_ops.Restore(
        file_pattern=checkpoint_path,
        tensor_name=self.bs.name,
        dt=np.float32,
        name='restore')
    self.bs.assign(restored)
    restored_tensors['bs'] = restored
    return restored_tensors

  @tf.function(input_signature=[])
  def initialize_weights(self):
    """Initializes the weights and bias of the head model.

    Returns:
      Map of initialized weight and bias.
    """
    self.ws.assign(tf.random.uniform((self.num_features, self.num_classes)))
    self.bs.assign(tf.random.uniform((1, self.num_classes)))
    return {'ws': self.ws, 'bs': self.bs}


def convert_and_save(saved_model_dir='saved_model'):
  """Converts and saves the TFLite Transfer Learning model.

  Args:
    saved_model_dir: A directory path to save a converted model.
  """
  model = TransferLearningModel()
  
  # Instantiate Dataset
  data = [[],[],[],[],[],[],[],[]]
  label = []
  
  for subject in range(10):
      # Extract raw EMG data images
      #data[gesture] = dataset_mat_CSL("CSL_HDEMG_Subject1_Session1/gest" + str(gesture+1) + ".mat", 1, NUM_FEATURES)
      #data[gesture] = dataset_mat_ICE("ICE_Lab_Database/1.20.21_Database/Training_Trimmed/001-00" + str(gesture+1) + "-001.mat", rows, columns)
      subject_data, subject_label = dataset_mat_Myo("Ninapro_DB5/s" +  str(subject+1) + "/S" + str(subject+1) + "_E1_A1.mat", window_length=ROWS)
      for i in range(8):
          data[i].extend(subject_data[i])
      label.extend(subject_label)
      
      subject_data2, subject_label2 = dataset_mat_Myo("Ninapro_DB5/s" +  str(subject+1) + "/S" + str(subject+1) + "_E2_A1.mat", window_length=ROWS)
      for i in range(8):
          data[i].extend(subject_data2[i])
      label.extend(subject_label2)
      
      subject_data3, subject_label3 = dataset_mat_Myo("Ninapro_DB5/s" +  str(subject+1) + "/S" + str(subject+1) + "_E3_A1.mat", window_length=ROWS)
      for i in range(8):
          data[i].extend(subject_data3[i])
      label.extend(subject_label3)
      
      #data[gesture] = dataset("Dataset/HandGesture0" + str(gesture+1) + ".txt", win_length, win_increment)
      # Extract MAV from raw data
      #data[gesture] = extractFeatures(data[gesture])
      #data[gesture] = extractFeaturesHD(data[gesture], rows, columns, win_length, win_increment)

  min_length = min([len(data[0]),len(data[1]),len(data[2]),len(data[3]),len(data[4]),len(data[5]),len(data[6]),len(data[7])])
  print(min_length)
  print([len(data[0]),len(data[1]),len(data[2]),len(data[3]),len(data[4]),len(data[5]),len(data[6]),len(data[7])])
  for epoch in range(2):
      for sample in range(min_length):
          for gesture in range(8):
              bottlenecks = model.load([data[gesture][sample]])['bottleneck']
              bottleneck = [0.,0.,0.,0.,0.,0.,0.,0.]
              for i in range(8):
                  bottleneck[i] = bottlenecks[0][i]
              current_label = [0,0,0,0,0,0,0,0]
              current_label[gesture] = 1
              model.train([bottleneck], [current_label])

  # for sample in range(len(data)):
  #     for gesture in range(8):
  #         print(gesture)
  #         print(model.infer([data[gesture][sample]])['output'])

  tf.saved_model.save(
      model,
      saved_model_dir,
      signatures={
          'load': model.load.get_concrete_function(),
          'train': model.train.get_concrete_function(),
          'infer': model.infer.get_concrete_function(),
          'save': model.save.get_concrete_function(),
          'restore': model.restore.get_concrete_function(),
          'initialize': model.initialize_weights.get_concrete_function(),
      })

  # Convert the model
  converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)
  converter.target_spec.supported_ops = [
      tf.lite.OpsSet.TFLITE_BUILTINS,  # enable TensorFlow Lite ops.
      tf.lite.OpsSet.SELECT_TF_OPS  # enable TensorFlow ops.
  ]
  converter.experimental_enable_resource_variables = True
  tflite_model = converter.convert()

  model_file_path = os.path.join('model.tflite')
  with open(model_file_path, 'wb') as model_file:
    model_file.write(tflite_model)


if __name__ == '__main__':
  convert_and_save()
