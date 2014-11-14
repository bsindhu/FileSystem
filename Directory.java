/** Directory class
*
* The "/" root directory maintains each file in a different directory entry that 
* contains its file name and the corresponding inode number.It receives the maximum number of 
* inodes to be created and keeps track of which inode numbers are in use.
*
*	Sindhuri Bolisetty 
*	CSS 430
*	Final Project
*
*@author Sindhuri Bolisetty
*@version 12/8/2013
*
* added Inode array, array synchronization, comments
*/

import java.util.*;

public class Directory {
    private static int maxChars = 30; // the max characters of each file name
    private int fsizes[];             // the actual size of each file name
    private char fnames[][];          // file names in characters
    
    // constructor
    public Directory ( int maxInumber ) {     
    	fsizes = new int[maxInumber];           // maxInumber = max # files
    	for ( int i = 0; i < maxInumber; i++ ){  // all file sizes set to 0
    			fsizes[i] = 0;
    	}
    	fnames = new char[maxInumber][maxChars];
    	String root = "/";                      // entry (inode) 0 is "/"
    	fsizes[0] = root.length();
    	root.getChars( 0, fsizes[0], fnames[0], 0 ); 
    }
    
    // converts and adds data in byte array to the directory
    public void bytes2directory( byte[] data ) {
    	int offset = 0;
	
    	// each offset is 4 bytes for the int in fsizes[]
    	for ( int i = 0; i < fsizes.length; i++, offset += 4 ){
    		// add sizes
    		fsizes[i] = SysLib.bytes2int( data, offset );
    	}
    	
    	// each offset is 2 bytes * maxChars (30) for the name in fnames[]
    	for ( int i = 0; i < fnames.length; i++, offset += maxChars * 2 ) {
    		String filename = new String( data, offset, maxChars * 2 );
	    
    		// add names
    		filename.getChars( 0, fsizes[i], fnames[i], 0 );
    	}
    }
 
    // converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    public byte[] directory2bytes() {
    	int offset = 0;
		
		// need a byte[]: number of bytes = (maxChars (30) * 2 bytes for char
		// plus 4 bytes for the int for fsizes[]) * fnames.length (fnames and fsizes
		// will be same length)
		int numBytes = ((maxChars * 2 + 4) * fnames.length);
		byte[] data = new byte[numBytes];
		
		// each offset is 4 bytes for the int in fsizes[]
		for ( int i = 0; i < fsizes.length; i++, offset += 4 ){
			// fill data with byte-converted sizes
			SysLib.int2bytes(fsizes[i], data, offset);
		}
		
		// each offset is 2 bytes * maxChars (30) for the name in fnames[]
        for ( int i = 0; i < fnames.length; i++, offset += maxChars * 2 ) {
        	// get names and convert to bytes
            byte[] filename = (new String(fnames[i])).getBytes();
            
            for (int j = 0; j < filename.length; j++) {
            	// fill data with the name bytes
                data[offset+j] = filename[j];
            }
        }
	return data;
    }
  
    /* allocate Inode for filename (-1 if none) 
       Filename is for the file to create.
       Allocates a new inode number for this filename*/
    public short ialloc ( String filename ){
    	short index = -1; // invalid
    	if((filename.length() > 0) && (filename.length() < maxChars)){
    		// Find first empty file location
    		for(short i = 0; i < fsizes.length; i++){
    			// search array and find 1st free space (index)
    			if(fsizes[i] == 0){ // free
    				//allocate the filename to that index
    				fsizes[i] = filename.length(); 
    				filename.getChars(0, fsizes[i], fnames[i], 0);
    				index = i; // update index
    			}
    		}
    	}
    	return index; // return -1 (error) or index of file
    }

    // When file system deletes a file, method returns the specified 
    // inumber (inode number). The corresponding file is deleted 
    // by the file system.
    public boolean ifree ( short iNumber ){
    	if(iNumber <= 0) {
    		return false; // already free
    	}
    	else { // reinitialize values at specified location
    		fnames[iNumber] = new char[maxChars];
    		fsizes[iNumber] = 0;
    		return true;
    	}     
    }
  
    // find Inode for filename (-1 if none)
    // returns the inumber corresponding to this filename
    public short namei( String filename ){
    	for (short i = 0; i < fsizes.length; i++) {
    		String name = new String(fnames[i], 0, fsizes[i]);
    		if (fsizes[i] > 0 && (filename.compareToIgnoreCase(filename) == 0)) {
    			return i; // the index of the filename (also inumber)
    		}
    	}
        return -1;
    }
    
}




































