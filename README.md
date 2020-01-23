# BrainStorm_Project

School project : Control a car with a brain waves

![alt text](https://raw.githubusercontent.com/KelianS/BrainStrom_Project/master/Doc/77230064_437840437171191_7725214017249083392_n.png)

Project finished on january 23,2020.

## Specifications :

```
  Base :
    Control a bluetooth 4.0 robot with an android smartphone.
    Create an app compatible to link and control the robot from our smartphone.
    Connect a 'MindWave Mobile 2 - NeuroSky' bluetooth 4.0 electroencephalogram to our app.
    Control the robot with data from the electroencephalogram.
    Add a database to memorise all actions
    Bonus : Use database to make the robot come back to where it start.
```

### Installing

What things you need to install the software and how to run :

```
  Prerequisites : If app already instaled it's recomended to unistall or delete storage.
```

```
  Instalation : With Android Studio, nothing specific.
```

## Running the app

```
  Step one : Wait untill app sucesfully scan your bluetooth device. (and then click on it)
    If it does not : Try to restart the app to make sure you accepted the uses of localisation data.
                     Try to reScan using the `Scan` button from trop right corner.
                     Make sure the robot is turned on.
                     
  Step two : Wait a fiew time untill your device is properly connected to your phone. (pretty quick here)
    If it does not : Try to restart using the button from top right corner.
    If it still does not : Retry from step One.
                     
  Step three : Turn on the bluetooth helmet and then hit `Headset` button
  
  Step four : Wait again till helmet is connected.. 
    If it does not : Retry from step three. (again)
    If Helmet is connected and show no signal or state :  Restart from step One. (sorry). 
```

## How to :

```
  On control activity (when device is connected) :
  
    Signal -> Quality of the positioning of the helmet.
    State -> Connection status of the helmet.
    Blink -> Virtual led that indicate if you blink (in yellow).
    
    Mod -> Indicate the mod you're running on (Grey = desactivated / 
                                               Yellow = Activated but not triggered /
                                               Green = Triggered)
    
      | Focus -> A number between [0;100] that indicate your focus level (with the helmet)
                 When your focus > 80, it unlock a boost mod for the motors to go faster than the others, but you need to stay focus ;)
    
      | Meditation -> A number between [0;100] that indicate your meditation level (with the helmet)
                When you're about to fall asleep(meditation >80), the robot take the control to drive itself and avoid a imminent collision
      
      | Reverse -> When triggered, the robot goes back from where it comes. To trigger it you need to do an eye blink.
    
    Buttons : 
    
      | DataBase -> Show database in another activity. (Only clickable when is connected to a device)
    
```

## Deployment

* 0.1.2
    * FINAL release.
    * CHANGE: Update docs.
    * CHANGES APP :
      * FINAL FIX : Batabase
      * ADD : Music on focus
      * FIX : Crashes of sensor Thread
    * CHANGES ROBOT :
      * FIX  : LOT OF PROBLEMS
      
* 0.1.1
    * CHANGES APP:
      * APP :
      * FINAL : Bluetooth sending data methodes.
      * FIX : DataBase with propper activity.
      * FIX : Headset data treatment and functions.
      * ADD : Return to 0 functions
      * ADD : Receiving sensor data.

* 0.1.0
    * The first proper release.
    * CHANGES APP :
      * REMOVE : "Unkown Devices".
      * CHANGES : Bluetooth sending data methodes.
      * ADD : DataBase (with activity not working).
      * ADD : Headset data treatment and new functions (as mod : focus and meditation)
    * CHANGES ROBOT :  
      * ADD : LiPo Battery.
* 0.0.2
    * First unstable release.
    * CHANGES APP:
      * ADD : Bluetooth Thread for sending.
      * ADD : SickBar and correspunding.
      * ADD : Bluetooth sending data methodes.
    * CHANGES ROBOT : 
      * ADD : First push full of digitalWrite/AnalogWrite, no security = to debug app
* 0.0.1
    * First push of base code.


## Built With the uses of : 

   StackOverflow :
   eg : https://stackoverflow.com/questions/3333658/how-to-make-a-vertical-seekbar-in-android
        https://stackoverflow.com/questions/24844514/database-in-android-studio/24844741
        https://stackoverflow.com/questions/2865315/threads-in-java
        https://stackoverflow.com/questions/1921514/how-to-run-a-runnable-thread-in-android-at-defined-intervals
        https://stackoverflow.com/questions/19205547/implementing-simple-seekbar-in-android
        ...
    Our teacher...

## Authors

* **Bernard Caron - Teacher** - *Initial Work*  
* **Gaspard Misery** - *Geii student* - [GitHub](https://github.com/GaspardCtrl)
* **Kelian Sermet** - *Geii student* - [GitHub](https://github.com/KelianS)
* **Benjamin Bouaziz** -Geii student* - [GitHub](https://github.com/BouazBenji)

## Acknowledgments

* Learning java
* Learning how to work in groups
* Learning how to manage time
* Learning uses of gant
* etc


![alt text](https://raw.githubusercontent.com/KelianS/BrainStrom_Project/master/Doc/Robot.png)
