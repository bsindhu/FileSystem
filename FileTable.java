/**
 * FileTable class
 *
 *	Sindhuri Bolisetty and David Lambert
 *	CSS 430
 *	Final Project
 *
 * @author Sindhuri Bolisetty
 * @version12/10/2013
 * 
 * Modified: David Lambert
* added Inode array, array synchronization, comments
 */

import java.util.*;

public class FileTable {
    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory
    public static Inode[] inodes; // vector of all Inodes
    
    // constructor
    public FileTable( Directory directory, Inode[] nodes ) { 
        // instantiate a file (structure) table
    	table = new Vector<FileTableEntry>( ); 
    	// receive a reference to the Directory from the file system
        dir = directory; 
        // set the Inode vector ref
        inodes = nodes;
        
    }                            

	
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    // Keeps track of thread usage of file and counts
    public FileTableEntry falloc( String fname, String mode ){
    	
    	synchronized(inodes){ // synchronize on the Inode array
    		// Inode to allocate
    		Inode inode = null;
    		short iNumber; 
        
    		// check if filename is root directory or not
    		if (fname.equals("/")) {
    			iNumber = 0;
    		} else {
    			iNumber = dir.namei(fname);
    		}
        	
    		// file does not exist
    		if(iNumber < 0){ 
    			if(mode.equals("r")){ // nothing to read
    				return null;
            		
    				// if write/append, create a file
    			} else { // "w", "w+", or "a"
    				// add to the directory
    				iNumber = dir.ialloc(fname); 
            		
    				if(iNumber < 0){ // out of space
    					return null; 
    				}
    			}
    			
    			inode = new Inode();
    			// new inode
    	   		// place the inode in the Inode array
        		inodes[iNumber] = inode;
            	
            //if the file exists
            } else { // (iNumber >= 0)
            	// instantiate the Inode with the retrieved iNumber
            	inode = new Inode(iNumber);
            }
    		
    		// save inode to disk
    		inode.toDisk( iNumber );   
        
    		// create file table entry
    		FileTableEntry ftEnt = new FileTableEntry( inode, iNumber, mode );
        
    		// add entry to FileTable
    		table.addElement( ftEnt );
        
    		// update the file instance count
    		ftEnt.inode.count++;
        
    		// return file table entry
    		return ftEnt;                  		
    	}
 
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty 
    }  


    // should be called before starting a format
    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    public synchronized boolean ffree( FileTableEntry ftEnt ) {
        // no ftEnt
    	if(ftEnt == null){
        	return false;
        }
        // remove entry
        boolean success = table.removeElement(ftEnt);
        
        // found
        if(success){
        	if(ftEnt.inode.count != 0){
        		ftEnt.inode.count--;
        	}
            ftEnt.inode.toDisk(ftEnt.iNumber);
            
            notify();
            return true;
            
        // not found
        } else {
        	return false; 
        }
    }

}
