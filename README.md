# BrainStorm_Project

School project : Control a car with a brain waves

![alt text](https://raw.githubusercontent.com/KelianS/BrainStrom_Project/master/Doc/77230064_437840437171191_7725214017249083392_n.png)

## Specifications :

//

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
                     
  Step two : Wait a fiew time untill your device is properly connected to your phone. (it may take a minute)
    If it does not : Try to restart using the button from trop right corner.
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
    
    Mod -> Indicate the mod you are running (...//...)
    
    Attention -> A number between ///;;;/// that indicate your level of focus (with the helmet)
    
    Meditation -> A number between ///;;;/// that indicate your level of ///...///unfocus (with the helmet)
    
    Buttons and actuator: 
    
      DataBase -> Show database in another activity. (Only clickable when is connected to a device)
      
      Focus -> ////...///
      
      Zen -> ///...///
    
```

## Deployment

* 0.1.2
    * FINAL release.
    * CHANGE: Update docs.
    * CHANGES APP :
      * FINAL FIX : Batabase
      * ADD : Music on focus
      * FIX : Crashes of sensor Thread
    *CHANGES ROBOT :
      
* 0.1.1
    * CHANGES APP:
      * APP :
      * FINAL : Bluetooth sending data methodes.
      * FIX : DataBase with propper activity.
      * FIX : Headset data treatment and functions.
      * ADD : Return to 0 functions
      * ADD : Receiving sensor data.
    * CHANGES ROBOT : 
        *
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
      *//Je sais pas
* 0.0.1
    * First push of base code.


## Built With the uses of : 

   StackOverflow :
   
   ...


## Authors

*// le prof? - *Initial work* - 
* **Gaspard Misery** - *Geii student* - [GitHub](https://github.com/GaspardCtrl)
* **Kelian Sermet** - *Geii student* - [GitHub](https://github.com/KelianS)

## Acknowledgments

* Learning
* Inspiration
* etc


![alt text](https://raw.githubusercontent.com/KelianS/BrainStrom_Project/master/Doc/Robot.png)
