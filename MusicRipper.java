import java.util.*;
import java.io.*;
import java.sql.*;
import java.nio.*;

public class MusicRipper{

public static void main(String[] argv) throws Exception {
    RandomAccessFile fin = new RandomAccessFile("ROM.gba", "rw");
    MusicRipper ripper = new MusicRipper();

    File file = new File("Output");
    file.mkdir();

    byte[] instTable = new byte[0];
    long startAddr = 0x0111E36C; // make this a user input at some point
    String songName = "Inescapable"; // this too

    instTable = ripper.readInstrumentTable(startAddr,fin);

    ripper.parseInstrumentTable(songName,instTable,startAddr);

    System.exit(0);
  }


public static byte[] combine(byte[] a, byte[] b){
    int length = a.length + b.length;
    byte[] result = new byte[length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
}


public void printByteArray(byte[] byteArray){
	for(int i = 0; i < byteArray.length; i++){
		System.out.printf("%02X ", byteArray[i]);
	}
}


public byte[] readInstrumentTable(long address, RandomAccessFile fin) throws Exception{
	byte[] instrumentTable = new byte[0];
	
	fin.seek(address);

	while(true){

	byte[] data = new byte[12];
	fin.read(data,0,12); // read next 12 bytes
	if (checkInstrumentTableEnd(data,fin)){
		return instrumentTable;
	}
	else{
		instrumentTable = combine(instrumentTable,data);
	}

	}

}


public boolean isSafetyPointer(long a, RandomAccessFile fin) throws Exception{
	// credit to 7743
 	long romlen = fin.length();
    return (a < 0x02000000 && a >= 0x100 && a < romlen);
}


public boolean checkInstrumentTableEnd(byte[] tableEntry, RandomAccessFile fin) throws Exception{
	// first check if it's a 0 terminator
	byte[] terminator = {0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0};

	if (Arrays.equals(tableEntry, terminator)){
		return true;
	}

	// if it's not a wave or noise instrument, check if the pointer (at +0x4) is invalid
	if (!checkWaveOrNoise(tableEntry[0])){
		byte[] checkArray = new byte[4];
		checkArray[0] = tableEntry[7]; // read backwards because pointer
		checkArray[1] = tableEntry[6];
		checkArray[2] = tableEntry[5];
		checkArray[3] = tableEntry[4];
		long checkLong = pointerToLong(checkArray);


		return !isSafetyPointer(checkLong,fin);
	}

	// if it is a wave or noise we assume it's okay
	return false;

}

public boolean checkWaveOrNoise(byte input){
	if (input == 0x1 || input == 0x2 || input == 0x4 || input == 0x9 || input == 0xA || input == 0xC){
		return true;
	}
	return false;
}

public boolean checkWaveMemory(byte input){
	if (input == 0x3){
		return true;
	}
	return false;
}

public boolean checkMultiSample(byte input){
	if (input == 0x40){
		return true;
	}
	return false;
}

public long pointerToLong(byte[] byteArray){

	long value = 0;

	int[] intArray = bytesToInts(byteArray);

	value |= (intArray[0] - 0x8) << (4 * 6);
	value |= intArray[1] << (4 * 4);
	value |= intArray[2] << (4 * 2);
	value |= intArray[3] << (4 * 0);

	return value;
}

public long bytesToLong(byte[] byteArray){	
	long value = 0;

	int[] intArray = bytesToInts(byteArray);

	value |= intArray[0] << (4 * 6);
	value |= intArray[1] << (4 * 4);
	value |= intArray[2] << (4 * 2);
	value |= intArray[3] << (4 * 0);

	return value;
}

public int[] bytesToInts(byte[] byteArray){

	int[] intArray = new int[byteArray.length];

	for (int i = 0; i < byteArray.length; i++){
		if ((int)byteArray[i] < 0){
			intArray[i] = 0xFF & ((int)byteArray[i]);
		}
		else{
			intArray[i] = (int)byteArray[i];
		}
	}

	return intArray;
}

public void generateBinFromInstAddr(String filename, long addr, RandomAccessFile fin) throws Exception{
	long popAddr = fin.getFilePointer(); // remember our posittion
	fin.seek(addr + 12); // lengthbyte

	byte[] instLengthOld = new byte[4];
	fin.read(instLengthOld,0,4);
	byte[] instLength = {instLengthOld[3],instLengthOld[2],instLengthOld[1],instLengthOld[0]};


	long length = bytesToLong(instLength);
	int len = (int)length;

	//if (!(len > 100000)){ // safety to not blow up

	byte[] data = new byte[len];
	fin.read(data,0,len); // get the data

	File file = new File("Output/" + filename + ".bin");

	try { 
  
        OutputStream os = new FileOutputStream(file); 
  		os.write(data); 
        os.close(); 
    } 
  
    catch (Exception e) { 
        System.out.println("Exception: " + e); 
    } 

	fin.seek(popAddr); // return to our position

	//}

	return;
}

public void generateBinFromWaveMemory(String filename, long addr, RandomAccessFile fin) throws Exception{
	long popAddr = fin.getFilePointer(); // remember our posittion
	fin.seek(addr);

	byte[] data = new byte[16];

	fin.read(data,0,16); // get the data

	File file = new File("Output/" + filename + ".bin");

	try { 
  
        OutputStream os = new FileOutputStream(file); 
  		os.write(data); 
        os.close(); 
    } 
  
    catch (Exception e) { 
        System.out.println("Exception: " + e); 
    } 

	fin.seek(popAddr); // return to our position

	return;
}

public void generateBinFromMultiSample(String filename, long addr, RandomAccessFile fin) throws Exception{
	long popAddr = fin.getFilePointer(); // remember our posittion
	fin.seek(addr);

	byte[] data = new byte[128];

	fin.read(data,0,128); // get the data

	File file = new File("Output/" + filename + ".bin");

	try { 
  
        OutputStream os = new FileOutputStream(file); 
  		os.write(data); 
        os.close(); 
    } 
  
    catch (Exception e) { 
        System.out.println("Exception: " + e); 
    } 

	fin.seek(popAddr); // return to our position

	return;
}

public byte[] getNextInstTableEntry(byte[] byteArray){
	return Arrays.copyOfRange(byteArray,0,12);
}

public byte[] advanceInstTable(byte[] byteArray){
	return Arrays.copyOfRange(byteArray,12,byteArray.length);
}

public byte[] getInstEntryFromAddress(long addr, RandomAccessFile fin) throws Exception{
	long popAddr = fin.getFilePointer(); // remember our posittion
	fin.seek(addr);

	byte[] data = new byte[16];
	fin.read(data,0,16); // get the 16 byte table entry

	fin.seek(popAddr); // return to our position

	return data;
}

public void parseInstrumentTable(String songName, byte[] instTable, long startAddr) throws Exception{

	File file = new File(songName + "Instrument.event");
	FileWriter fileWriter = new FileWriter(songName + "Instrument.event");
	PrintWriter printWriter = new PrintWriter(fileWriter);

	RandomAccessFile fnew = new RandomAccessFile("ROM.gba", "rw");

	printWriter.println("// File output by Instrument Table ripper");
	printWriter.println("// Program by Pikmin1211 with credit to 7743");
	printWriter.println("\nALIGN 4");
	printWriter.println(songName + "InstrumentTable:");

	long[] instAddresses = new long[(instTable.length/12)];
	long[] waveAddresses = new long[(instTable.length/12)];
	long[] sampleAddresses = new long[(instTable.length/12)];
	int i = 0;

	while(instTable.length != 0){

		byte[] currEntry = getNextInstTableEntry(instTable);
		instTable = advanceInstTable(instTable);

		if (checkMultiSample(currEntry[0])){ // if multi sample
			printTableEntryMultiSample(currEntry,songName,i,printWriter);
			byte[] checkArray = new byte[4];
			checkArray[0] = currEntry[11]; // read backwards because pointer
			checkArray[1] = currEntry[10];
			checkArray[2] = currEntry[9];
			checkArray[3] = currEntry[8];
			sampleAddresses[i] = pointerToLong(checkArray);
			generateBinFromMultiSample((songName + "MultiSample" + "Instrument" + i),sampleAddresses[i],fnew);
		}
		else if (checkSoundPointer(currEntry,startAddr)){ // if the instrument points to the table
			printTableEntrySelfPointer(currEntry,songName,i,printWriter);
		}
		else if (checkWaveOrNoise(currEntry[0])){ // if has no instrument pointer
			printTableEntryPattern(currEntry,songName,i,printWriter);
		}
		else if (checkWaveMemory(currEntry[0])){ // if wave memory
			printTableEntry(currEntry,(songName + "WaveMemory"),i,printWriter);
			byte[] checkArray = new byte[4];
			checkArray[0] = currEntry[7]; // read backwards because pointer
			checkArray[1] = currEntry[6];
			checkArray[2] = currEntry[5];
			checkArray[3] = currEntry[4];
			waveAddresses[i] = pointerToLong(checkArray);
			generateBinFromWaveMemory((songName + "WaveMemory" + "Instrument" + i),waveAddresses[i],fnew);
		}
		else { // regular instrument
			printTableEntry(currEntry,songName,i,printWriter);
			byte[] checkArray = new byte[4];
			checkArray[0] = currEntry[7]; // read backwards because pointer
			checkArray[1] = currEntry[6];
			checkArray[2] = currEntry[5];
			checkArray[3] = currEntry[4];
			instAddresses[i] = pointerToLong(checkArray);
			generateBinFromInstAddr((songName + "Instrument" + i),instAddresses[i],fnew);
		}

		i += 1;

	}

	printWriter.println("InstrumentTableTerminator");

	for(int j = 0; j < instAddresses.length; j++){
		if (instAddresses[j] != 0){
			printWriter.println("");
			printWriter.println("ALIGN 4");
			printWriter.println(songName + "Instrument" + j + ":");
			printInstrumentEntry(instAddresses[j],fnew,printWriter);
			printWriter.println("#incbin \"Output/" + songName + "Instrument" + j + ".bin\"");
	}
}

	for(int k = 0; k < waveAddresses.length; k++){
		if (waveAddresses[k] != 0){
			printWriter.println("");
			printWriter.println("ALIGN 4");
			printWriter.println(songName + "WaveMemory" + "Instrument" + k + ":");
			printWriter.println("#incbin \"Output/" + songName + "WaveMemory" + "Instrument" + k + ".bin\"");
	}


}

	for(int m = 0; m < sampleAddresses.length; m++){
		if (sampleAddresses[m] != 0){
			printWriter.println("");
			printWriter.println("ALIGN 4");
			printWriter.println(songName + "MultiSample" + "Instrument" + m + ":");
			printWriter.println("#incbin \"Output/" + songName + "MultiSample" + "Instrument" + m + ".bin\"");
	}


}


	printWriter.close();
	return;


}

public boolean checkSoundPointer(byte[] byteArray, long startAddr){

	byte[] bytes = new byte[4];
	bytes[0] = byteArray[7]; // read backwards because pointer
	bytes[1] = byteArray[6];
	bytes[2] = byteArray[5];
	bytes[3] = byteArray[4];

	long value = pointerToLong(bytes);

	return value == startAddr;
}

public String byteToString(byte hexByte){
	return "0x" + String.format("%02X", hexByte);
}

public void printTableEntry(byte[] tableEntry, String songName, int entryNo, PrintWriter printWriter){
	printWriter.println(
		"InstrumentTableEntry(" +
		byteToString(tableEntry[0]) + ", " +
		byteToString(tableEntry[1]) + ", " +
		byteToString(tableEntry[3]) + ", " +
		songName + "Instrument" + entryNo + ", " +
		byteToString(tableEntry[8]) + ", " +
		byteToString(tableEntry[9]) + ", " +
		byteToString(tableEntry[10]) + ", " +
		byteToString(tableEntry[11]) + ")"
		);
	return;
}

public void printTableEntrySelfPointer(byte[] tableEntry, String songName, int entryNo, PrintWriter printWriter){
	printWriter.println(
		"InstrumentTableEntry(" +
		byteToString(tableEntry[0]) + ", " +
		byteToString(tableEntry[1]) + ", " +
		byteToString(tableEntry[3]) + ", " +
		songName + "InstrumentTable, " +
		byteToString(tableEntry[8]) + ", " +
		byteToString(tableEntry[9]) + ", " +
		byteToString(tableEntry[10]) + ", " +
		byteToString(tableEntry[11]) + ")"
		);
	return;
}

public void printTableEntryPattern(byte[] tableEntry, String songName, int entryNo, PrintWriter printWriter){
	printWriter.println(
		"InstrumentTableEntryNoPointer(" +
		byteToString(tableEntry[0]) + ", " +
		byteToString(tableEntry[1]) + ", " +
		byteToString(tableEntry[3]) + ", " +
		byteToString(tableEntry[4]) + ", " +
		byteToString(tableEntry[8]) + ", " +
		byteToString(tableEntry[9]) + ", " +
		byteToString(tableEntry[10]) + ", " +
		byteToString(tableEntry[11]) + ")"
		);
	return;

}

public void printTableEntryMultiSample(byte[] tableEntry, String songName, int entryNo, PrintWriter printWriter){
	printWriter.println(
		"InstrumentTableEntryMultiSample(" +
		byteToString(tableEntry[0]) + ", " +
		byteToString(tableEntry[1]) + ", " +
		byteToString(tableEntry[3]) + ", " +
		songName + "InstrumentTable, " +
		songName + "MultiSample" + "Instrument" + entryNo + ")"
		);
	return;
}

public void printInstrumentEntry(long address, RandomAccessFile fin, PrintWriter printWriter) throws Exception{
	// this is a bit hacky oh well
	byte[] data = getInstEntryFromAddress(address,fin);

	printWriter.println(
		"BYTE " +
		byteToString(data[0]) + " " + 
		byteToString(data[1]) + " " + 
		byteToString(data[2]) + " " + 
		byteToString(data[3]) + " " + 
		byteToString(data[4]) + " " + 
		byteToString(data[5]) + " " + 
		byteToString(data[6]) + " " + 
		byteToString(data[7]) + " " + 
		byteToString(data[8]) + " " + 
		byteToString(data[9]) + " " + 
		byteToString(data[10]) + " " + 
		byteToString(data[11]) + " " + 
		byteToString(data[12]) + " " + 
		byteToString(data[13]) + " " + 
		byteToString(data[14]) + " " + 
		byteToString(data[15])
	);

	return;
}

}