# AndroidApplication
The source code of Landmark Application for Android Development. For identification process, offline inference is implemented.
The trained model should be embedded first.

The pb model is not included in the directory.
## Usage
In order to get the relevant models, users should train CNN models with their own data set. The source code of transfer learning based on ResNet and DenseNet is provided [here](https://github.com/xiranx/Landmark-identification-for-Android).

Hint: Please pay attention to the image input size requirement. 

ResNet&DenseNet:244x244. Inception:299x299. MobileNet:160x160. 
