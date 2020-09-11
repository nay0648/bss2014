# 频域盲源分离算法研究及其在高速列车噪声成分分离中的应用

北京交通大学

博士学位论文

作者：纳跃跃

导师：于剑

# 中文摘要

盲源分离问题研究的是如何将各个源信号从观测得到的混合信号中分离出来，所谓“盲源”，是指源信号和混合环境都未知。盲源分离技术有许多潜在的应用，例如语音增强、语音识别、脑电信号分析及核磁共振成像分析、特征提取、地质勘探、图像去噪、高光谱图像处理等。
在本文中，我们主要关注的是频域盲源分离算法。由于信号在介质中的传播速率是有限的，所以在真实环境中所观测得到的信号往往是各个源信号以及它们的延时、衰减、回响的叠加，即信号是卷积混合的。频域盲源分离算法利用短时傅里叶变换（Short Time Fourier Transform, STFT）将时域上的卷积混合转换为频域上的瞬时混合，然后利用目前研究比较成熟的瞬时盲源分离算法在各个频段上进行分离，所以，整个分离问题可以得到极大的简化。但是，频域盲源分离算法也会受到所谓的“排列歧义性”的影响：在输出最终结果之前必须将各个频段上分离好的信号重新调整为相同的输出顺序，也就是说，在各个频段分离完毕之后，频域盲源分离算法还需要一种排列算法作为后期处理步骤来解决排列歧义性。
本文主要介绍了作者在对频域盲源分离算法的研究过程中所开展的四项工作，包括：
1.	使用核方法和谱方法解决排列问题。在频域盲源分离算法中，聚类技术被广泛用于解决排列歧义性。然而，目前的研究中仍然存在一些问题尚未解决，例如需要处理长条形分布的数据，以及需要考虑排列问题特有的限制条件等。由于核方法和谱方法在机器学习和数据挖掘中的广泛应用，所以我们尝试使用这些技术来解决排列问题。在该项工作中，我们根据排列问题特有的限制条件修改了kernel k-means方法，并且使用谱聚类的思想对核方法的原理进行了解释。另外，我们还根据排列问题的具体特点提出了几种核映射的构造方法以提高排列算法的性能。
2.	基于子频带和子空间非线性映射的IVA（Independent Vector Analysis）算法。IVA算法是近年来才提出的一种新技术，该方法的应用之一就是解决频域盲源分离问题。和传统的按频段瞬时分离加排列算法的方法相比，IVA最大的优点在于它能够在分离的过程中同时进行排列，所以IVA之后不需要再使用额外的排列算法进行后期处理。在这项工作中，我们提出了对IVA方法的两项改进：首先提出了一种新的子频带构造方法，在分离过程中IVA将以子频带为单位从高频到低频进行分离，而子频带中数据的强相关性将有利于解决排列问题；第二，为了增强算法的稳定性以及减少噪声的影响，IVA的非线性映射将在由数据的相关矩阵的最大特征值对应的特征向量张成的一维子空间上计算。这两项改进同时使用的话不但能够提升IVA算法的分离性能，而且还能增强算法的稳定性。
3.	频域盲源分离算法的性能评价。为了开发更好的盲源分离算法，如何对算法的分离性能进行定量评价也是一个值得深入研究的问题。在本工作中我们针对频域盲源分离算法的特点对现有的评价方法进行了改进：首先从混合和分离系统中计算出统一的信号能量传递网络，或是在频域上从源信号和分离所得的信号中估计出能量传递网络的参数。然后再根据前一步的计算结果对信号的能量进行分解，从而计算不同的评价指标。在该工作中提出的方法尤其适合于在模拟实验环境中对频域盲源分离算法进行性能评价。
4.	高速列车噪声成分分离。高速列车噪声是影响车内旅客舒适度和铁路沿线居民生活质量的关键因素，如何有效的降低噪声是高速列车设计者们所关心的问题之一。研究表明，高速列车噪声是由车体振动、轮轨噪声、气动噪声、设备噪声等因素混合而成的综合结果，如果能将各种噪声成分从测量得到的信号中分离出来将对列车的减振降噪设计起到一定的参考作用。由于盲源分离算法在语音分离任务中有许多成功应用的先例，而这两类问题之间又存在一定的相似性，所以我们尝试使用盲源分离技术来对高速列车噪声成分进行分离。在这项工作中一共分离了两类噪声：一是使用频响特性估计的方法实现了透射噪声和结构噪声的分离，二是使用盲源分离技术对车内噪声成分进行了分离。
本文的所有工作以及本文实验中用到的所有对比算法都集成到了一个统一的实验平台中，该平台由Java语言开发，源代码可以供研究者们自由下载。

关键词：盲源分离；频域；排列歧义性；独立向量分析；高速列车噪声
分类号：TP391 

# ABSTRACT

Blind source separation (BSS) aims at recovering individual source signals from their mixed observations, the word “blind” means that neither the sources nor the mixing environment is known. There are many potential applications of BSS techniques such as speech enhancement, robust speech recognition, analyzing EEG or fMRI signals, feature extraction, geological exploration, image denoising, hyperspectral image processing, etc.
In this dissertation, we mainly focus on frequency domain blind source separation (FDBSS) algorithms. Since signals propagate in specified velocity in real-word mixing environment, different sources are mixed with each other, as well as their delays, attenuations, and reverberations, i.e., they are mixed in a convolutive manner. FDBSS algorithms utilize short time Fourier transform (STFT) to convert time domain convolutive mixture to frequency domain instantaneous mixture, then signals can be separated by well-studied instantaneous BSS algorithms in each frequency bin, so the entire separation problem is simplified. However, FDBSS algorithms suffer from the well-known “permutation ambiguity”: separated signals in each frequency bin must be rearranged to the same order before output the final results, so, a permutation algorithm is required for this purpose as a postprocessing step after frequency bin-wise separation.
The main contributions of this dissertation are as follows:
1.	Kernal and spectral methods for solving the permutation problem. Clustering techniques are broadly used to solve the permutation problem in FDBSS algorithms, however, some challenges still exist, for example, elongated datasets should be handled, and constraint from the background knowledge must be considered. Inspired by various successful applications of kernel and spectral clustering methods in machine learning and data mining community, we try to solve the permutation problem by these methods. In this work, the weighted kernel k-means algorithm is modified according to the specific requirement of the permutation problem, and the spectral interpretation of the kernel approach is also investigated. In addition, several kernel construction approaches are proposed to improving the permutation performance.
2.	Independent vector analysis using subband and subspace nonlinearity. Independent vector analysis (IVA) is a recently proposed technique, which can solve the FDBSS problem. Compared with the traditional frequency bin-wise instantaneous separation plus permutation correction approach, the largest advantage of IVA is that the permutation problem is directly addressed by IVA rather than resorting to the use of an ad hoc permutation resolving algorithm after a separation of the sources in multiple frequency bands. In this work, two updates for IVA are presented. First, a novel subband construction method is introduced, IVA will be conducted in subbands from high frequency to low frequency, and the fact that the inter-frequency dependencies in subbands are stronger allows a more efficient approach to the permutation problem. Second, to improve robustness and decrease noise, the IVA nonlinearity is calculated only in the signal subspace, which is defined by the eigenvector associated with the largest eigenvalue of the signal correlation matrix. When the two updates are used together, both separation performance and algorithm robustness are dramatically improved.
3.	Performance evaluation methods for FDBSS algorithms. In order to develop better BSS algorithms, how to evaluate the algorithm performance becomes a problem worthy to be investigated. In this work, we mainly focus on the evaluation problem for FDBSS algorithms. First, the uniform energy flow network is calculated from the mixing and the demixing system, or estimated from the original and the estimated source signals in frequency domain. Then, signals are decomposed according to their energy flow, and several performance indices are derived from the decomposition. The proposed method is especially suitable for BSS performance evaluation in simulated environments.
4.	High speed train noise components separation. High speed train noise level is an important factor with respect to passenger comfort and life quality of residents along the railway, thus determining how to attenuate the noise level is an important research direction that train designers care about. Studies show that train noise is a kind of mixed signal which is made up of train body vibration, rolling noise, aerodynamic noise, device noise, etc., separating individual noise components from the overall observations will provide some guide to train noise reduction design. Since BSS algorithms have many successful applications in speech and audio separation tasks, which are similar to the circumstance in the train, we try to use these techniques to perform train noise components separation. Two kinds of noise components are separated in this work: first, train transmission noise and structural noise are separated by a system identification method; then, train interior noise components are separated by BSS algorithms.
All the works in this dissertation and all compared algorithms are integrated in a unified platform for BSS research and application, this platform is developed in Java, and the source code is available for public.

KEYWORDS：Blind source separation; Frequency domain; Permutation ambiguity; Independent vector analysis; High speed train noise
CLASSNO：TP391
