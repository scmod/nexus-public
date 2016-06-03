package com.nexus.plugin.file.resource;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;

import com.google.common.base.Strings;

@Named("file")
@Singleton
public class FileGavCalculator implements GavCalculator {

	@Override
	public Gav pathToGav(String path) {
		String s = path.startsWith("/") ? path.substring(1) : path;

		int g, a, v, c, e;
		String groupId = null, artifactId = null, version = null, fileName = null, classifier = null, extention = null;
		g = s.indexOf('/');
		if (g != -1) {
			groupId = s.substring(0, g).replace('/', '.');
			fileName = s.substring(g + 1);
			a = s.indexOf('/', g + 1);
			if (a != -1) {
				artifactId = s.substring(g + 1, a);
				fileName = s.substring(a + 1);
				v = s.indexOf('/', a + 1);
				if (v != -1) {
					version = s.substring(a + 1, v);
					fileName = s.substring(v + 1);
				}
			}
		}
		boolean checksum = false;
		boolean signature = false;
		Gav.HashType checksumType = null;
		Gav.SignatureType signatureType = null;

		String tmpFileName = fileName;
		if (tmpFileName.endsWith(".md5")) {
			checksum = true;
			checksumType = Gav.HashType.md5;
			extention = "md5";
		} else if ((e = tmpFileName.lastIndexOf('.')) != -1
				&& e < (tmpFileName.length() - 1)) {
			extention = tmpFileName.substring(e + 1);
		}
		if (extention != null)
			tmpFileName = tmpFileName.substring(0, tmpFileName.length()
					- (extention.length() + 1));

		if ((c = tmpFileName.lastIndexOf('-')) != -1
				&& c < (tmpFileName.length() - 1)) {
			classifier = tmpFileName.substring(c + 1);
			tmpFileName = tmpFileName.substring(0, c);
		}

		return new Gav(groupId, artifactId, version, classifier, extention,
				null, null, fileName, checksum, checksumType, signature,
				signatureType);
	}

	@Override
	public String gavToPath(Gav gav) {
		StringBuilder path = new StringBuilder("/");

	    path.append(gav.getGroupId().replaceAll("(?m)(.)\\.", "$1/")); // replace all '.' except the first char

	    path.append("/");

	    path.append(gav.getArtifactId());

	    path.append("/");

	    path.append(gav.getVersion());

	    path.append("/");

	    String fileName = gav.getName();
	    int dotPos = 0;
	    
	    if(!Strings.isNullOrEmpty(fileName)) {
	    	//原本没有后缀名...慢慢拼
	    	if((dotPos = fileName.lastIndexOf('.')) == -1) {
	    		if(StringUtils.isNotBlank(gav.getClassifier()))
	    			fileName += "-" + gav.getClassifier();
	    		if(StringUtils.isNotBlank(gav.getExtension()))
	    			fileName += "." + gav.getExtension();
	    	} else {
	    		//原本就有后缀名,就无视给出的extention,也不知道有没有直接用'.'结尾的文件名~
	    		if(StringUtils.isNotBlank(gav.getClassifier()))
	    			fileName = fileName.substring(0, dotPos) + "-" + gav.getClassifier() + fileName.substring(dotPos);
	    	}
	    }
	    path.append(fileName);

	    return path.toString();
//		if (gav.getName() != null && gav.getName().trim().length() > 0) {
//			return gav.getName();
//		} else {
//			StringBuilder path = new StringBuilder(gav.getArtifactId());
//
//			path.append("-");
//
//			path.append(gav.getVersion());
//
//			if (gav.getClassifier() != null
//					&& gav.getClassifier().trim().length() > 0) {
//				path.append("-");
//
//				path.append(gav.getClassifier());
//			}
//
//			if (gav.getExtension() != null) {
//				path.append(".");
//
//				path.append(gav.getExtension());
//			}
//
//			if (gav.isSignature()) {
//				path.append(".");
//
//				path.append(gav.getSignatureType().toString());
//			}
//
//			if (gav.isHash()) {
//				path.append(".");
//
//				path.append(gav.getHashType().toString());
//			}
//
//			return path.toString();
//		}
	}
}
