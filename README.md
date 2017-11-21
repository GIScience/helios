# HELIOS
The Heidelberg LiDAR Operations Simulator ([HELIOS](http://www.geog.uni-heidelberg.de/gis/helios_en.html)) is a software package for interactive real-time simulation and visualization of terrestrial, mobile and airborne laser scanning surveys written in Java.

## HELIOS-FWF
HELIOS ver. 2.0 - Full-WaveForm implementation within the Heidelberg LiDAR Operations Simulator.

### Features

The following FWF features are included to the latest FWF HELIOS build:
- BRDF reflectance model.
- Gaussian decomposition to extract multiple echo returns (point coordinates,intensity,width,return number). Based on <a href="https://github.com/odinsbane/least-squares-in-java">least square fitting library</a>.
 
Future features will include:
- More sophisticated atmospheric scattering model.
- Per-scanner based input waveform profile of the outgoing pulse.
 
### Configuration variables (for \<FWFSettings \> in \<survey\> XML file)
- numTimeBins (integer) - Number of bins to discretize the temporal domain of individual return.
- numFullwaveBins (integer) - Total number of bins to discretize the entire temporal domain of FWF signal.
- winSize (integer) - Number of neigbhouring samples to check (i.e. 1D window size) when finding local maxima for Gaussian decomposition.
- peakEnergy (double) - Scanner's peak emitting energy [W].
- apertureDiameter (double) - Scanner's aperture diameter [m].
- atmosphericVisbility (double) - Decrease of intensity due atmospheric scattering. Within range [0, 1].
- scannerEfficiency (double) - Efficiency of the scanner. Within range of [0, 1]. 
- scannerWaveLength (double) - Scanner's operating wavelength [ns].
- beamDivergence_rad (double) - Scanner's beam divergence angle [rad].  Used if not defined in \<scanner>.
- pulseLength_ns (double) - Scanner's pulse length [ns]. Used if not defined in \<scanner>.
- beamSampleQuality (integer) - Quality of spatial discretization of the subrays (higher value = more subrays within the laser cone). Default set to 3.

### Modification of MTF (obj material) file

 - helios_reflectance (double) - Define the reflectance (albedo) of the given material. Within range [0, 1].

### Output point format

*point.x point.y point.z intensity echoWidth FWFIndex returnNumber returnNumPerPulse objectId*

- point (double) - Coordinates of 3D captured points from echo returns in FWF signal.
- intensity (double) - Intensity of the return [W].
- echoWidth (double) - The full width at half maximum of the echo-return [ns].
- returnNumber (integer) - The corresponding return number.
- returnNumPerPulse (integer) - Number of returns per pulse.
- FWFIndex (integer) - The index of the FWF signal.
- objectId (string) - The id of the hit object.

### Output FWF format

*FWFIndex beamOrigin.x beamOrigin.y beamOrigin.z beamDir.x beamDir.y beamDir.z minTime maxTime gpsTime FWFIntensities*

- FWFIndex (integer) - The index of the FWF signal (in order to match corresponding points)
- beamOrigin (double) - 3D point of the beam's origin.
- beamOrigin (double) - 3D vector of the beam's direction.
- minTime (double) - FWF signal beginning time [ns].
- maxTime (double) - FWF signal ending time [ns].
- gpsTIme (integer) - Simulated GPS time.
- FWFIntensities (double) - Intensities of the entire FWF signal. Number defined with numFullwaveBins.
