void TimerInit() {
  cli();//stop interrupts

  //set timer1 interrupt at 300Hz
  TCCR1A = 0;// set entire TCCR1A register to 0
  TCCR1B = 0;// same for TCCR1B
  TCNT1  = 0;//initialize counter value to 0
  // set compare match register for 300hz increments
  OCR1A = 51;//300Hz
  // turn on CTC mode
  TCCR1B |= (1 << WGM12);
  // Set CS12 and CS10 bits for 1024 prescaler
  TCCR1B |= (1 << CS12) | (1 << CS10);  
  // enable timer compare interrupt
  TIMSK1 |= (1 << OCIE1A);
  
  sei();//allow interrupts
  
}

// the setup routine runs once when you press reset:
void setup() {
  TimerInit();
  // initialize serial communication at 115200 bits per second:
  Serial.begin(115200);
  //pinMode(13, OUTPUT);    // sets the digital pin 13 as output for logic analyzing
}

ISR(TIMER1_COMPA_vect){//timer1 interrupt at 300Hz
  //int8_t sensorValue = (analogRead(A0) >> 2) - 125;   //No Amplification
  //int8_t sensorValue = (analogRead(A0) >> 1) - 250; //x2 Amplification
  int8_t sensorValue0 = analogRead(A0)-500;          //x4 Amplification
  int8_t sensorValue1 = analogRead(A1)-500;          //x4 Amplification
  int8_t sensorValue2 = analogRead(A2)-500;          //x4 Amplification
  int8_t sensorValue3 = analogRead(A3)-500;          //x4 Amplification
  //digitalWrite(13, !digitalRead(13));
  Serial.write(sensorValue0);
  Serial.write(sensorValue1);
  Serial.write(sensorValue2);
  Serial.write(sensorValue3);
}


// the loop routine runs over and over again forever:
void loop() {
    
}
