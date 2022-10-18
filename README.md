<h1 id="title">AttractorSimulator</h1>

<img src="https://user-images.githubusercontent.com/34401005/196242230-7e08eb3b-73c7-456e-8b37-b490cee37111.jpg" height=150 id="icon"></img>

A simple 2d and 3d chaos attractor visualiser written in java and libGDX

## Features
- A lot of different attractors
  - 2D
    - De Jong
    - Svensson
    - Clifford
    - Boo
    - Ghost
    - Product
    - Popcord
    - 12 variable polynomial
  - 3D 
    - Three-Scroll Unified
    - Halvorsen
    - Lorenz
    - Aizawa 
- A variety of different color palettes
- Extensible through json configs
- 19 presets to play with
- Word to attractor converter, visualize your name!
```json
"PRODUCT": {
  "displayName": "Product",
  "rules": [
    "3.1415926 * sin(a0*y) * cos(a1*x)",
    "3.1415926 * sin(a2*x) * cos(a3*y)"
  ],
  "variableCount": 4,
  "defaultVariables": [5.13, 1.3, 3.0, 2.61],
  "variableLimits": [-7, 7],
  "useZInTextInterpretation": true,
  "dimensions": [-3.1415918, -3.1415918, 3.1415918, 3.1415918],
  "contrast": 0.89
},
```

## Installation
Clone this repository and import into **Android Studio** or **Intellij Idea**
```bash
https://github.com/dgudim/AttractorSimulator
```

> Currently, there is no ui to switch between 2D and 3D attractors, so to do so, you will need to change one method in <br>
> ```./core/src/com/deo/attractor/Launcher.java```<br>
> specifically, this one 
> ```java
> public void create() {
>   this.setScreen(new SimulationScreen2D());
> }
> ```
> change it to ```SimulationScreen3D``` to play with 3D attractors, i was too lazy to make a ui

| [Here is an example of what is possible with this software](https://youtu.be/8ttGBReE5gg) |
| - |
| <a href="https://youtu.be/8ttGBReE5gg"><img src="https://user-images.githubusercontent.com/34401005/196383368-2985c626-5d16-491e-9df5-40822e540c7f.png" id="thumb"></img></a> |


