package com.github.maxopoly.angeliacore.binary;

import com.github.maxopoly.angeliacore.libs.nbt.NBTCompound;
import com.github.maxopoly.angeliacore.libs.nbt.NBTParser;
import com.github.maxopoly.angeliacore.model.item.ItemStack;
import com.github.maxopoly.angeliacore.model.item.Material;
import com.github.maxopoly.angeliacore.model.location.Location;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

/**
 * Wraps a byte array to ease parsing data out of it. Internally a pointer is moved forward every time data is read
 * through one of the methods provided.
 *
 * For example if a byte array contained 2 long, you could create an instance of this class and call getLong() twice to
 * extract those. The funcionality provided by this class goes far past parsing out numbers though, it includes
 * everything that's encoded in binary in minecraft packets like positions, NBT etc.
 *
 *
 * Data is read from index 0 to data.length - 1, all numbers are assumed to big endian (as used in minecrafts protocol).
 *
 */
public class BinaryReadOnlyData {

	protected byte[] data;
	protected int dataPointer;

	public BinaryReadOnlyData(byte[] data) {
		this.data = data;
		this.dataPointer = 0;
	}

	/**
	 * Reads a single signed long (8 bytes)
	 *
	 * @return Read long
	 * @throws EndOfPacketException
	 *             Thrown if less than 8 bytes are left in the data, meaning it can't contain a valid long
	 */
	public long readLong() throws EndOfPacketException {
		if (dataPointer + 8 > data.length) {
			throw new EndOfPacketException("Tried to read long, but data pointer was at " + dataPointer + " out of "
					+ data.length);
		}
		long value = 0;
		for (int i = 0; i < 8; i++) {
			value = (value << 8) + (((long) data[i + dataPointer]) & (long) 0xff);
		}
		dataPointer += 8;
		return value;
	}

	/**
	 * Reads a single signed short (2 bytes)
	 *
	 * @return Read short
	 * @throws EndOfPacketException
	 *             Thrown if less than 2 bytes are left in the data, meaning it can't contain a valid short
	 */
	public short readShort() throws EndOfPacketException {
		if (dataPointer + 2 > data.length) {
			throw new EndOfPacketException("Tried to read short, but data pointer was at " + dataPointer + " out of "
					+ data.length);
		}
		byte[] intData = Arrays.copyOfRange(data, dataPointer, dataPointer + 2);
		short result = ByteBuffer.wrap(intData).order(ByteOrder.BIG_ENDIAN).getShort();
		dataPointer += 2;
		return result;
	}

	/**
	 * Reads a single IEEE double (8 bytes)
	 *
	 * @return Read double
	 * @throws EndOfPacketException
	 *             Thrown if less than 8 bytes are left in the data, meaning it can't contain a valid double
	 */
	public double readDouble() throws EndOfPacketException {
		if (dataPointer + 8 > data.length) {
			throw new EndOfPacketException("Tried to read double, but data pointer was at " + dataPointer + " out of "
					+ data.length);
		}
		byte[] doubleData = Arrays.copyOfRange(data, dataPointer, dataPointer + 8);
		double result = ByteBuffer.wrap(doubleData).order(ByteOrder.BIG_ENDIAN).getDouble();
		dataPointer += 8;
		return result;
	}

	/**
	 * Reads a single IEEE float (4 bytes)
	 *
	 * @return Read float
	 * @throws EndOfPacketException
	 *             Thrown if less than 4 bytes are left in the data, meaning it can't contain a valid float
	 */
	public float readFloat() throws EndOfPacketException {
		if (dataPointer + 4 > data.length) {
			throw new EndOfPacketException("Tried to read float, but data pointer was at " + dataPointer + " out of "
					+ data.length);
		}
		byte[] doubleData = Arrays.copyOfRange(data, dataPointer, dataPointer + 4);
		float result = ByteBuffer.wrap(doubleData).order(ByteOrder.BIG_ENDIAN).getFloat();
		dataPointer += 4;
		return result;
	}

	/**
	 * Reads a single signed byte
	 *
	 * @return Read byte
	 * @throws EndOfPacketException
	 *             Thrown if the data buffer was already used up completly
	 */
	public byte readByte() throws EndOfPacketException {
		if (dataPointer + 1 > data.length) {
			throw new EndOfPacketException("Tried to read byte, but data pointer was at " + dataPointer + " out of "
					+ data.length);
		}
		return data[dataPointer++];
	}

	/**
	 * Reads the given amount of bytes
	 *
	 * @return Read byte array
	 * @throws EndOfPacketException
	 *             Thrown if the data buffer does not contain enough bytes
	 */
	public byte[] readBytes(int amount) throws EndOfPacketException {
		if (dataPointer + amount > data.length) {
			throw new EndOfPacketException("Tried to read " + amount + " bytes, but data pointer was at " + dataPointer
					+ " out of " + data.length);
		}
		byte[] resultData = new byte[amount];
		for (int i = 0; i < amount; i++) {
			resultData[i] = data[i + dataPointer];
		}
		dataPointer += amount;
		return resultData;
	}

	/**
	 * Reads an item stack, see the protocol wiki for the exact specification of item stack encoding
	 *
	 * @return Item stack read
	 * @throws EndOfPacketException
	 *             Thrown if the data left does not represent a proper item stack
	 */
	public ItemStack readItemStack() throws EndOfPacketException {
		short id = readShort();
		if (id == -1) {
			return new ItemStack(Material.EMPTY_SLOT);
		}
		byte count = readByte();
		short dmg = readShort();
		NBTParser parser = new NBTParser(Arrays.copyOfRange(data, dataPointer, data.length));
		NBTCompound compound = parser.parse();
		dataPointer += parser.getLength();
		return new ItemStack(Material.getByID(id), count, dmg, compound);
	}

	/**
	 * Reads a byte array, which is prefixed by it's own length in the form of a varint
	 *
	 * @return Read byte array
	 * @throws EndOfPacketException
	 *             Thrown if the varint is invalid or not enough data exists
	 */
	public byte[] readByteArray() throws EndOfPacketException {
		return readBytes(readVarInt());
	}

	/**
	 * Reads a UUID (always 16 bytes)
	 *
	 * @return Read UUID
	 * @throws EndOfPacketException
	 *             Thrown if not at least 16 byte are left
	 */
	public UUID readUUID() throws EndOfPacketException {
		return new UUID(readLong(), readLong());
	}

	/**
	 * Reads a single string from the input data. Strings are made up of an initial varint denoting the length of the
	 * string and the actual string encoded in utf-8
	 *
	 * @return String read
	 * @throws EndOfPacketException
	 *             Thrown if packet ended before String ended
	 */
	public String readString() throws EndOfPacketException {
		int charAmount = readVarInt();
		return new String(readBytes(charAmount));
	}

	/**
	 * Reads a single signed int (4 bytes)
	 *
	 * @return Read int
	 * @throws EndOfPacketException
	 *             Thrown if less than 4 bytes are left in the data, meaning it can't contain a valid int
	 */
	public int readInt() throws EndOfPacketException {
		if (dataPointer + 4 > data.length) {
			throw new EndOfPacketException("Tried to read int, but data pointer was at " + dataPointer + " out of "
					+ data.length);
		}
		byte[] intData = Arrays.copyOfRange(data, dataPointer, dataPointer + 4);
		int result = ByteBuffer.wrap(intData).order(ByteOrder.BIG_ENDIAN).getInt();
		dataPointer += 4;
		return result;
	}

	/**
	 * Reads a single boolean, datawise each boolean is represented through an entire byte where:
	 *
	 * x == 0 --> false;
	 *
	 * x != 0 --> true
	 *
	 * @return Read boolean
	 * @throws EndOfPacketException
	 *             Thrown if less than 4 bytes are left in the data, meaning it can't contain a valid int
	 */
	public boolean readBoolean() throws EndOfPacketException {
		byte b = readByte();
		if (b == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Reads an encoded location (always 8 bytes) and parses out it's x, y and z, which are always integers;
	 *
	 * @return Read location
	 * @throws EndOfPacketException
	 *             Thrown if less than 8 bytes are left in the data, meaning it can't contain a valid location
	 */
	public Location readPosition() throws EndOfPacketException {
		long val = readLong();
		int x = (int) (val >> 38);
		int y = (int) ((val >> 26) & 0xFFF);
		int z = (int) (val << 38 >> 38);
		return new Location(x, y, z);
	}

	/**
	 * Reads an array of NBT objects, which is prefixed by its own length (in the form of a varint)
	 *
	 * @return Parsed NBT array
	 * @throws EndOfPacketException
	 *             Thrown if data is invalid
	 */
	public NBTCompound[] readNBTArray() throws EndOfPacketException {
		int length = readVarInt();
		NBTCompound[] result = new NBTCompound[length];
		for (int i = 0; i < length; i++) {
			NBTParser parser = new NBTParser(Arrays.copyOfRange(data, dataPointer, data.length));
			result[i] = parser.parse();
			dataPointer += parser.getLength();
		}
		return result;
	}

	/**
	 * Reads a varInt from the input data. While normal int are always 4 byte, varint have a dynamic length. They use
	 * the 8th bit of every byte to signal whether another byte follows. This allows making your data smaller
	 * dynamically and is used a lot in minecrafts protocol.
	 *
	 * @return int read
	 * @throws IOException
	 *             thrown if varInt could not be read or was too big
	 */
	public int readVarInt() {
		int i = 0;
		int j = 0;
		while (true) {
			int k = data[dataPointer++];
			i |= (k & 0x7F) << j++ * 7;
			if (j > 5)
				throw new RuntimeException("VarInt too big");
			if ((k & 0x80) != 128)
				break;
		}
		return i;
	}

}
