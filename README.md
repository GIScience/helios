# helios

> Heidelberg LiDAR Operations Simulator (HELIOS)

Software package for interactive real-time simulation and visualization of terrestrial, mobile and airborne laser scanning surveys written in Java.

Official website: http://www.geog.uni-heidelberg.de/gis/helios_en.html

[![HELIOS](http://img.youtube.com/vi/1SOg7b5q4ak/0.jpg)](https://www.youtube.com/watch?v=1SOg7b5q4ak "HELIOS")

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [Documentation](#documentation)
- [Authorship](#authorship)
- [License](#license)

## Background

LiDAR (Light and Ranging Detection) technology has now become the quintessential technique for collecting geospatial data from the earth's surface. This project allows the generation of simulated LiDAR point clouds.

## Install

First install the dependencies, then compile the source code, and finally execute it.

### Dependencies

- Oracle Java 1.8
- Maven 3
- Git

In Ubuntu the these packages can be installed by:
```bash
sudo apt-get install oracle-java8-installer maven git
```

### Compilation

Inside the root folder simply execute:
```bash
mvn package
```

### Eclipse IDE

To add this project to Eclipse go to *File > Import > Existing Maven Projects* and select the root folder.

## Usage

HELIOS supports both single-ray and full-waveform simulations. Currently the default mode is the full-waveform. The selection between this two methods is done in the source code when creating the detector object in *XmlAssetsLoader.java*. 

### Input

The argument of the program is the survey XML file.  See  [Wiki: Basic input data](https://github.com/GIScience/helios/wiki/Quick-start-guide#basic-input-data) for further details. 


### Execution

From the root folder run:

```bash
java -jar target/helios.jar <survey-file>
```
To use the batch mode (no visualization):

```bash
java -jar target/helios.jar <survey-file> headless
```
### Output

Output files are generated inside *output/Survey Playback* folder.

* Point cloud: File *legxxx_points.xyz*  separated by spaces
Fields:  
X Y Z I ECHO_WIDTH RN NOR FWF_ID OBJ_ID
Example:  
-4.615 15.979 2.179 4.0393 1.4317 1 1 214275 1
* Waveform: File *legxxx_points.xyzfullwave.txt*  separated by spaces
See [FWF.md](FWF.md) for further details. 

## Documentation

See the [Wiki](https://github.com/GIScience/helios/wiki)  for futher details.


## Authorship

GIScience Research Group
Institute of Geography
University of Heidelberg
Maintainers: @sebastian-bechtold @nlukac @deuxbot 

Research paper:

Bechtold, S. & Höfle, B. (2016): HELIOS: A Multi-Purpose LiDAR Simulation Framework for Research, Planning and Training of Laser Scanning Operations with Airborne, Ground-Based Mobile and Stationary Platforms. ISPRS Annals of Photogrammetry, Remote Sensing and Spatial Information Sciences. Vol. III-3, pp. 161-168. http://dx.doi.org/10.5194/isprs-annals-III-3-161-2016

## License

GNU General Public License v3.0


