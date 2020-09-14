# HELIOS

> Heidelberg LiDAR Operations Simulator (HELIOS)

HELIOS is a software package for interactive real-time simulation and visualization of terrestrial, mobile and airborne laser scanning surveys written in Java. Official website: https://www.uni-heidelberg.de/helios

[![HELIOS](http://img.youtube.com/vi/1SOg7b5q4ak/0.jpg)](https://www.youtube.com/watch?v=1SOg7b5q4ak "HELIOS")

## Table of Contents
- [Install](#install)
- [Usage](#usage)
- [Documentation](#documentation)
- [Authorship](#authorship)
- [License](#license)

## Install
The pre-built project is available [here](https://heibox.uni-heidelberg.de/f/06bb612921/?raw=1) with a test scene and survey. Note that for faster simulations **building the project is recommended**. 

To build the project, first install the dependencies, then compile the source code, and finally execute it.

### Requisites
- Oracle Java 1.8
- Maven 3

You can use ```java -version``` and ```mvn -v``` to check if you already have installed those on your computer.

#### Linux/Ubuntu:
They can be installed by:

```bash
sudo apt-get install oracle-java8-installer maven
```
#### Windows:
In Windows installation is less straightforward:  
- Java JDK  
	1. Download the [Java JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and install it  
	2. Create the environment variable *JAVA_HOME* to point to the JDK folder  
	(e .g: *JAVA_HOME=C:\Program Files\Java\jdk1.8.0_151*)  
	3. Add to the *PATH* the value *%JAVA_HOME%\bin*  
- Maven  
	1. Download the [Maven](https://maven.apache.org/download.cgi) *binary* zip  
	2. Unzip it and move it to the desired folder (e. g. *C:\apache-maven-3.5.2*)  
	3. Add to the *PATH* the bin folder inside (e. g. *C:\apache-maven-3.5.2\bin*)  

### Compilation

Download the source code from this web site or from git:

```bash
git clone https://github.com/GIScience/helios.git
```
Then inside the root folder simply execute:
```bash
mvn package
```

### Eclipse IDE

To add this project to Eclipse go to *File > Import > Existing Maven Projects* and select *helios* folder.  
You may also want to disable the spell checker: right click in the project > *Checkstyle > Deactivate Checkstyle*

## Usage

HELIOS supports both single-ray and full-waveform simulations. Currently the default mode is the full-waveform. The selection between this two methods is done in the source code when creating the detector object in [*XmlAssetsLoader.java*](src/main/java/de/uni_hd/giscience/helios/assetsloading/XmlAssetsLoader.java). 

### Input

The argument of the program is the survey XML file, also the 3D models of the scene pointed in the survey are needed. See [Wiki: Basic input data](https://github.com/GIScience/helios/wiki/Quick-start-guide#basic-input-data) for further details. 

This [Blender addon](https://github.com/neumicha/Blender2Helios) can be used to convert Blender scenes to HELIOS scenes.

### Execution

From the root folder run:

```bash
java -jar target/helios.jar <survey-file>
```
To run the example survey:

```bash
java -jar target/helios.jar data/surveys/demo/tls_arbaro_demo.xml 
```

To use the batch mode (no visualization):

```bash
java -jar target/helios.jar <survey-file> headless
```
### Output

Output files are generated inside *output/Survey Playback* folder.

* Point cloud: File named *legxxx_points.xyz* separated by spaces  where *xxx* is the leg number.  
Fields:  
X Y Z I ECHO_WIDTH RN NOR FWF_ID OBJ_ID  
Example:   
-4.615 15.979 2.179 4.0393 1.4317 1 1 214275 1
* Waveform: File named *legxxx_points.xyzfullwave.txt* separated by spaces  where *xxx* is the leg number.  
See [FWF.md](FWF.md) for further details. 

## Documentation

See [Wiki](https://github.com/GIScience/helios/wiki).

## Research using HELIOS

Backes, D., Smigaj, M., Schimka, M., Zahs, V., Grznárová, A., and Scaioni, M. (2020): River morphology monitoring of a small-scale alpine riverbed using drone photogrammetry and LiDAR, Int. Arch. Photogramm. Remote Sens. Spatial Inf. Sci., XLIII-B2-2020, 1017–1024, DOI: [10.5194/isprs-archives-XLIII-B2-2020-1017-2020](https://doi.org/10.5194/isprs-archives-XLIII-B2-2020-1017-2020). 

Li, L., Mu, X., Soma, M., Wan,P., Qi, J., Hu, R., Zhang, W., Tong, Y., Yan, G. (2020): An Iterative-Mode Scan Design of Terrestrial Laser Scanning in Forests for Minimizing Occlusion Effects, IEEE Transactions on Geoscience and Remote Sensing, DOI: [10.1109/TGRS.2020.3018643](https://doi.org/10.1109/TGRS.2020.3018643).

Park, M., Baek, Y., Dinare, M., Lee, D., Park, K.-H., Ahn, J., Kim, D., Medina, J., Choi, W.-J., Kim, S., Zhou, C., Heo, J. & Lee, K. (2020): Hetero-integration enables fast switching time-of-flight sensors for light detection and ranging. In: Sci Rep 10, 2764 (2020), pp. 1-8. DOI: [10.1038/s41598-020-59677-x](https://doi.org/10.1038/s41598-020-59677-x).

Wang, D. (2020): Unsupervised semantic and instance segmentation of forest point clouds. In: ISPRS Journal of Photogrammetry and Remote Sensing 165 (2020), pp. 86-97. DOI: [10.1016/j.isprsjprs.2020.04.020](https://doi.org/10.1016/j.isprsjprs.2020.04.020). 

Wang, D., Schraik, D., Hovi, A., Rautiainen, M. (2020): Direct estimation of photon recollision probability using terrestrial laser scanning. In: Remote Sensing of Environment 247 (2020), pp. 1-12. DOI: [10.1016/j.rse.2020.111932](https://doi.org/10.1016/j.rse.2020.111932). 

Zhu, X., Liu, J., Skidmore, A.K., Premier, J., Heurich, M. (2020): A voxel matching method for effective leaf area index estimation in temperate deciduous forests from leaf-on and leaf-off airborne LiDAR data. In: Remote Sensing of Environment, 240. DOI: [10.1016/j.rse.2020.111696](https://doi.org/10.1016/j.rse.2020.111696).

Lin, C.-H. & Wang, C.-K. (2019): [Point Density Simulation for ALS Survey](https://www.geog.uni-heidelberg.de/md/chemgeo/geog/gis/mmt2019-lin_and_wang_compr.pdf). In: Proceedings of the 11th International Conference on Mobile Mapping Technology (MMT2019), Shenzhen, China. pp. 157-160. 

Liu, J., Skidmore, A.K., Wang, T., Zhu, X., Premier, J., Heurich, M., Beudert, B. &amp; Jones, S. (2019): Variation of leaf angle distribution quantified by terrestrial LiDAR in natural European beech forest. In: ISPRS Journal of Photogrammetry and Remote Sensing, 148, pp. 208-220. DOI: [10.1016/j.isprsjprs.2019.01.005](https://doi.org/10.1016/j.isprsjprs.2019.01.005).

Liu, J., Wang, T., Skidmore, A.K., Jones, S., Heurich, M., Beudert, B. &amp; Premier, J. (2019): Comparison of terrestrial LiDAR and digital hemispherical photography for estimating leaf angle distribution in European broadleaf beech forests. In: ISPRS Journal of Photogrammetry and Remote Sensing, 158, pp. 76-89. DOI: [10.1016/j.isprsjprs.2019.09.015](https://doi.org/10.1016/j.isprsjprs.2019.09.015).

Martínez Sánchez, J., Váquez Álvarez, Á., López Vilariño, D., Fernández Rivera, F., Cabaleiro Domínguez, J.C., Fernández Pena, T. (2019): Fast Ground Filtering of Airborne LiDAR Data Based on Iterative Scan-Line Spline Interpolation. In: Remote Sensing, 11(19), pp. 23 (2256). DOI: [10.3390/rs11192256](https://doi.org/10.3390/rs11192256).

Previtali, M., Díaz-Vilariño, L., Scaioni, M. &amp; Frías Nores, E. (2019): [Evaluation of the Expected Data Quality in Laser Scanning Surveying of Archaeological Sites](http://hdl.handle.net/11311/1124569). In: 4th International Conference on Metrology for Archaeology and Cultural Heritage, Florence, Italy, 4-6 December 2019, pp. 19-24. 

Xiao, W., Zaforemska, A., Smigaj, M., Wang, Y. &amp; Gaulton, R. (2019): Mean Shift Segmentation Assessment for Individual Forest Tree Delineation from Airborne Lidar Data. In: Remote Sensing, 11(11), pp. 19 (1263). DOI: [10.3390/rs11111263](https://doi.org/10.3390/rs11111263).

Zhang, Z., Li, J., Guo, Y., Yang, C., &amp; Wang, C. (2019): 3D Highway Curve Reconstruction From Mobile Laser Scanning Point Clouds. In: IEEE Transactions on Intelligent Transportation Systems. DOI: [10.1109/TITS.2019.2946259](https://doi.org/10.1109/TITS.2019.2946259).

Hämmerle, M., Lukač, N., Chen, K.-C., Koma, Zs., Wang, C.-K., Anders, K., &amp; Höfle, B. (2017): Simulating Various Terrestrial and UAV LiDAR Scanning Configurations for Understory Forest Structure Modelling. In: ISPRS Ann. Photogramm. Remote Sens. Spatial Inf. Sci., IV-2/W4, pp. 59-65. DOI: [10.5194/isprs-annals-IV-2-W4-59-2017](https://doi.org/10.5194/isprs-annals-IV-2-W4-59-2017).

Rebolj, D., Pučko, Z., Babič, N.Č., Bizjak, M. & Mongus, D. (2017). Point cloud quality requirements for Scan-vs-BIM based automated construction progress monitoring</a>. In: Automation in Construction, 84, pp. 323-334. DOI: [10.1016/j.autcon.2017.09.021](https://doi.org/10.1016/j.autcon.2017.09.021).

Bechtold, S., Hämmerle, M. &amp; Höfle, B. (2016): [Simulated full-waveform laser scanning of outcrops for development of point cloud analysis algorithms and survey planning: An application for the HELIOS lidar simulation framework](http://lvisa.geog.uni-heidelberg.de/papers/2016/Bechtold_et_al_2016.pdf). In: Proceedings of the 2nd Virtual Geoscience Conference, Bergen, Norway, 21-23 September 2016, pp 57-58.


## Authorship

3DGeo Research Group  
Institute of Geography  
Heidelberg University

[http://www.uni-heidelberg.de/3dgeo](http://www.uni-heidelberg.de/3dgeo)

### Citation

Bechtold, S. & Höfle, B. (2016): HELIOS: A Multi-Purpose LiDAR Simulation Framework for Research, Planning and Training of Laser Scanning Operations with Airborne, Ground-Based Mobile and Stationary Platforms. ISPRS Annals of Photogrammetry, Remote Sensing and Spatial Information Sciences. Vol. III-3, pp. 161-168. DOI: [10.5194/isprs-annals-III-3-161-2016](http://dx.doi.org/10.5194/isprs-annals-III-3-161-2016)

If you use HELIOS in your work, please cite:

```
@Article{isprs-annals-III-3-161-2016,
AUTHOR = {Bechtold, S. and H\"ofle, B.},
TITLE = {{HELIOS}: A Multi-Purpose LiDAR Simulation Framework for Research, Planning and Training of Laser Scanning Operations with Airborne, Ground-Based Mobile and Stationary Platforms},
JOURNAL = {ISPRS Annals of Photogrammetry, Remote Sensing and Spatial Information Sciences},
VOLUME = {III-3},
YEAR = {2016},
PAGES = {161--168},
URL = {https://www.isprs-ann-photogramm-remote-sens-spatial-inf-sci.net/III-3/161/2016/},
DOI = {10.5194/isprs-annals-III-3-161-2016}
}
```

We are happy if you are using HELIOS in your work - [let us know](https://www.uni-heidelberg.de/helios)!

### Maintainers

[@sebastian-bechtold](https://github.com/sebastian-bechtold) [@nlukac](https://github.com/nlukac) [@kathapand](https://github.com/kathapand) [@deuxbot](https://github.com/deuxbot) [@lrg-bhoefle](https://github.com/lrg-bhoefle)

The first HELIOS version with full-waveform support is available in [this](https://github.com/nlukac/helios-FWF) repository maintained by [@nlukac](https://github.com/nlukac).

## License

See [LICENSE.md](LICENSE.md).
