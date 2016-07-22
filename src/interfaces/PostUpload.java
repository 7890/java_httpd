package interfaces;

import java.io.File;

//=============================================================================
public interface PostUpload
{
	public String postUploadProcess(File file, String originalFilename) throws Exception;
}
//EOF
