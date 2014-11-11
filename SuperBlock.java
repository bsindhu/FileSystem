
/**
* class SuperBlock
* 
* Superblock is used to describe 
* (1) the number of disk blocks, 
* (2) the number of inodes, 
* (3) the block number of the head block of the free list.
*
*
* @author Sindhuri Bolisetty
* @version 12/6/2013
* 
* added Inode array, array synchronization, comments
*/

import java.util.*;

class SuperBlock {
    private static final int defaultInodeBlocks = 32;
    private static final int inodeSize = 32; // bytes
    public int totalBlocks;		// total disk blocks
    public int totalInodes;		// total number inodes
    public int freeList;		// the number of the first free block on freelist
    
    // constructor
    // constructs superblock and formats disk to specified size
    public SuperBlock(int diskSize) {
    	//read the superblock from disk
        byte[] superBlock = new byte[Disk.blockSize]; 
        SysLib.rawread(0, superBlock);
        // convert intialization variables to ints
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        SysLib.cout("totalBlocks in SuperBlock() now " + totalBlocks + "\n");
        totalInodes = SysLib.bytes2int(superBlock, 4);
        SysLib.cout("totalInodes in SuperBlock() now " + totalInodes + "\n");
        freeList = SysLib.bytes2int(superBlock, 8);
        // disk contents are valid
        if (totalBlocks == diskSize && totalInodes > 0 && freeList >= 2) {
            return;
        } else {
	    // need to format disk
	    totalBlocks = diskSize;
	    SysLib.cout("default format ( " + defaultInodeBlocks + " )\n");
	    format(defaultInodeBlocks);
        }
    }
    
    // default format
    public void format(){
    	format(defaultInodeBlocks);
    }
    
    // formats the superblock to specified number of inodes
    public void format(int numInodes){
    	// set inodes
    	totalInodes = numInodes;
    	
    	// set the free list and sync()
    	freeList = (numInodes / 16) + 1;
    	sync();
    	
    	// set blocks to point to next free block and write to disk
    	byte[] newBlock = new byte[Disk.blockSize];
    	for(int i = freeList; i < totalBlocks - 1; i++){
    		SysLib.int2bytes(i + 1, newBlock, 0);
    		SysLib.rawwrite(i, newBlock);
    	}
    	// set last block to point invalid and write to disk
    	SysLib.int2bytes(-1, newBlock, 0);
    	SysLib.rawwrite(totalBlocks - 1, newBlock);
    	
    }
    
    // create an Inode vector of capacity totalInodes
    // To be used to hold all Inodes (in file system and file table)
    public Inode[] getInodes(){
    	Inode[] nodes = new Inode[totalInodes];
    	
    	for(short i = 0; i < totalInodes; i++){
    		nodes[i] = new Inode(i);
    		
    	}
    	
    	return nodes;
    }

    // Write back superblock (totalBlocks, inodeBlocks, and freeList) to disk
    // to maintain integrity of superblock
	public void sync(){
		// create a buffer, convert variables to bytes, and write to disk
	    byte[] superBlock = new byte[Disk.blockSize];
	    SysLib.int2bytes(totalBlocks, superBlock, 0);
	    SysLib.int2bytes(totalInodes, superBlock, 4);
	    SysLib.int2bytes(freeList, superBlock, 8);
	    SysLib.rawwrite(0, superBlock);
	}

    // Finds first (top) block in free list and returns its number
    public int getFreeBlock(){
    	int temp = freeList;    //variable to store current head for the list
    	// if return block is invalid, no free block number to return
    	if(temp == -1){
    		return temp;	// return invalid
    	}
    	
    	byte[] block = new byte[Disk.blockSize]; //read the block
    	SysLib.rawread(freeList, block); 
    	
    	// get the next free block from the one to be removed and return it
    	temp = SysLib.bytes2int(block,0); 
    	return temp;
    }


    // returns a block to the free list (deallocates it).
    // Returns true if block returned, false if not
    public boolean returnBlock(int blockNumber){
    	// can't return a block from the Inode blocks
    	if(blockNumber < (totalInodes / 16) + 1){
    		return false;
    	}
    	// create block buffer and convert freeList to bytes
    	byte[] returnedBlock = new byte[Disk.blockSize];
    	SysLib.int2bytes(freeList, returnedBlock, 0);
    	
    	// write to Disk and update the freeList
    	SysLib.rawwrite(blockNumber, returnedBlock);
    	freeList = blockNumber;
    	
    	return true;
    }
}
