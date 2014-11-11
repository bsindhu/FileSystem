/** Inode.java
*
* Sindhuri Bolisetty and David Lambert
* CSS 430
* Final Project
*
* @author Sindhuri Bolisetty
* @version 12/10/2013
* added array, array synchronization, comments
*/

import java.util.*;

public class Inode {
    private final static int iNodeSize = 32;       // node size: 32 bytes
    private final static int directSize = 11;      // direct pointers
    private final static int DISKSIZE = 1000;		// size of the disk
    
    public int length;                             // file size in bytes
    public short count;                            // # file-table entries
    public short flag;
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // indirect pointer
      
    // default constructor (new Inode)
    Inode( ) { 
    	
        	// initialize
        	length = 0;
        	count = 0;
        	flag = 0;
        	
        	for ( int i = 0; i < directSize; i++ )
        		direct[i] = -1;
        	indirect = -1;

    }
    
	// constructor (retrieves Inode from disk)
    Inode( short iNumber ) { 

        	// get the block the Inode is in
        	int blkNumber = (iNumber / 16) + 1; // 16 Inodes per block, superblock at 0
        	
        	// read the block that has the Inode
        	byte[] data = new byte[Disk.blockSize]; 
        	SysLib.rawread( blkNumber, data );
        	
        	// find offset in block
        	int offset = ( iNumber % 16 ) * iNodeSize; 
        	
        	// read and set variables, update offset for each
        	length = SysLib.bytes2int( data, offset );
        	offset += 4;
        	count = SysLib.bytes2short( data, offset );
        	offset += 2;
        	flag = SysLib.bytes2short( data, offset );
        	offset += 2;
        	
        	// set direct pointers
        	for ( int i = 0; i < directSize; i++ ) {
        		direct[i] = SysLib.bytes2short( data, offset );
        		offset += 2;
        	}
        	
        	// set the indirect pointer
        	indirect = SysLib.bytes2short( data, offset );

    }
    
    // writes an Inode to disk
    public void toDisk(int iNumber)  {
    	// changes Inode
    	// synchronized to Inode array in File Table
    	synchronized (FileTable.inodes){
        	// error if invalid iNumber
        	if(iNumber < 0 || iNumber >= DISKSIZE){
        		return;
        	}
        	
        	// get the block the Inode is in
        	int blockNumber = (iNumber / 16) + 1;
        	
        	//read the whole block from the disk
        	byte[] data = new byte[Disk.blockSize];  
        	SysLib.rawread(blockNumber, data);
        	
        	// find the offset (in the block)
        	int offset = (iNumber * iNodeSize) % Disk.blockSize;
        	
        	// add the current variables to the data buffer
        	// update the offset for each one
        	SysLib.int2bytes(length, data, offset);
        	offset += 4;
        	SysLib.short2bytes(count, data, offset);
        	offset += 2;
        	SysLib.short2bytes(flag, data, offset);
        	offset += 2;
        	
        	// add the direct pointers to the data buffer
        	for (int i = 0; i < directSize; i++, offset += 2) {
        		SysLib.short2bytes(direct[i], data, offset);
        	}
        	
        	// add the indirect pointer to the data buffer
        	SysLib.short2bytes(indirect, data, offset);
        	
        	// write the Inode to the disk
        	SysLib.rawwrite(blockNumber, data);
    	}

    }

    
    // find target block of Inode with specified offset of file.
    // Returns block number if found (int), -1 if not found
    public int findTargetBlock(int offset) {
    	
    	// find offset in block	
    	int blockNum = offset / Disk.blockSize; 
	
    	// if block in direct range
    	if(blockNum < direct.length){
    		// return the direct pointer
    		return direct[blockNum]; 
	    
    	// if block in indirect range
    	} else {	// blockNum > direct.length
    		if(indirect < 0){ // nothing here
    			return -1;
    		}
    		
    		// get and read the index block
    		byte[] indirectBlock = new byte[Disk.blockSize];
    		SysLib.rawread(indirect, indirectBlock);
    		
    		// return the indirect pointer
    		return SysLib.bytes2short(indirectBlock, ((blockNum - directSize) * 2));
    	}
    	
    }

    // registers the index block and updates indirect pointer.
    // Returns true if able to register, otherwise false.
    public boolean registerIndexBlock(short indexBlockNumber) {
    	// changes Inode
    	// synchronized to Inode array in File Table
    	synchronized (FileTable.inodes){
        	// if index block exists, error
        	if(indirect > -1){
        		return false;
        		
        	// if index block does not exist, register it
        	} else {
        		// set indirect pointer and create block buffer
        		indirect = indexBlockNumber;
        		byte[] newBlock = new byte[Disk.blockSize];
        		
        		// convert invalid values to bytes (initial values of block)
        		for(int i = 0; i < (Disk.blockSize / 2); i++){
        			SysLib.short2bytes((short) -1, newBlock, i * 2);
        		}
        		// write index block to disk
        		SysLib.rawwrite(indirect, newBlock);
        		
        		return true;
        	}
    	}

    }

    // Returns the index block number (int)
    public short getIndexBlockNumber() {
    	return indirect;
    }
    
    // Registers the specified block. Attempts to register block as direct
    // then indirect. Superblock is passed in case an index block is required.
    // Returns true for success, false for failure
    public boolean registerTargetBlock(final SuperBlock superblock, int seekPtr, 
    		short newBlock) {
    	// changes Inode
    	// synchronized to Inode array in File Table
    	synchronized (FileTable.inodes){
        	// find the block
        	int targetPtr = seekPtr / Disk.blockSize;
        	
        	// if within range of direct ptrs
        	if(targetPtr < directSize){
        		if(direct[targetPtr] >= 0){ // something's already there
        			return false;
        		}
        		
        		// if invalid
        		if((direct[(targetPtr -1)] == -1) && (targetPtr > 0)){
        			return false;
        		}
        		// register block
        		direct[targetPtr] = newBlock;
        		return true;
        	} 
        	
        	// indirect: if we don't have an index block, get one and register
        	if(indirect < 0){
        		short indexblock = (short) superblock.getFreeBlock();
        		if(registerIndexBlock(indexblock) == false){
        			return false;
        		}
        	}
        	
        	// read the index block and set a indirect ptr
        	byte[] buffer = new byte[Disk.blockSize];
        	SysLib.rawread(indirect, buffer);
        	int indirectBlockPtr = targetPtr - directSize;
        	
        	// if a block is already listed
        	if(SysLib.bytes2short(buffer, indirectBlockPtr * 2) > 0){
        		return false;
        	}
        	
        	// convert block number to bytes
        	SysLib.short2bytes(newBlock, buffer, indirectBlockPtr * 2);
        	// write to disk
        	SysLib.rawwrite(indirect, buffer);
        	
        	return true;
    	}
    }
    
    // unregisters the index block (makes it invalid).
    // Returns byte[] from index block with pointers to indirect blocks.
    public byte[] unregisterIndexBlock() {
    	// changes Inode
    	// synchronized to Inode vector in File Table
    	synchronized (FileTable.inodes){
        	// if index block already invalid
        	if (indirect == -1) {
                return null;
            }
        	// create a buffer and read the index block
            byte[] indirectBlock = new byte[Disk.blockSize];
            SysLib.rawread(indirect, indirectBlock);
            
            // set the indirect pointer to invalid
            indirect = -1;
            
            // return the buffer (byte[]) from index block
            return indirectBlock;
    	}

    }
    
}
