package me.josscoder.bedrockschem;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.snappy.SnappyCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

import java.io.*;

public class WriterBatch {

    @Getter
    private int countBlocks = 0;
    private final BinaryStream blocksStream = new BinaryStream();

    public void addBlocksFrom3DGrid(Vector3 cornerOne, Vector3 cornerTwo, Level level) {
        int minX = (int) Math.min(cornerOne.x, cornerTwo.x);
        int minY = (int) Math.min(cornerOne.y, cornerTwo.y);
        int minZ = (int) Math.min(cornerOne.z, cornerTwo.z);

        int maxX = (int) Math.max(cornerOne.x, cornerTwo.x);
        int maxY = (int) Math.max(cornerOne.y, cornerTwo.y);
        int maxZ = (int) Math.max(cornerOne.z, cornerTwo.z);

        int iX = (maxX - minX + 1);
        int iY = (maxY - minY + 1);
        int iZ = (maxZ - minZ + 1);

        Vector3 vector3 = new Vector3(iX, iY, iZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int blockId = level.getBlockIdAt(x, y, z);
                    if (blockId == Block.AIR) continue;

                    int blockData = level.getBlockDataAt(x, y, z);

                    blocksStream.putInt(blockId);
                    blocksStream.putInt(blockData);

                    Vector3 newVector = vector3.clone().add(new Vector3(
                            (x - minX),
                            (y - minY),
                            (z - minZ)
                    ));

                    blocksStream.putInt(newVector.getFloorX());
                    blocksStream.putInt(newVector.getFloorY());
                    blocksStream.putInt(newVector.getFloorZ());

                    countBlocks++;
                }
            }
        }
    }

    public byte[] getStream() {
        BinaryStream stream = new BinaryStream();
        stream.putInt(countBlocks);
        stream.put(blocksStream.getBuffer());

        return stream.getBuffer();
    }

    public void saveAsFile(File file) throws IOException, StreamBatchException {
        saveAsFile(file, CompressionAlgorithm.ZSTD);
    }

    public void saveAsFile(File file, CompressionAlgorithm algorithm) throws IOException, StreamBatchException {
        if (!file.exists()) file.createNewFile();

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            CompressorOutputStream compressorOutputStream;
            switch (algorithm) {
                case ZSTD:
                    compressorOutputStream = new ZstdCompressorOutputStream(fileOutputStream);
                    break;
                case SNAPPY:
                    compressorOutputStream = new SnappyCompressorOutputStream(fileOutputStream, 8192);
                    break;
                case ZLIB:
                    compressorOutputStream = new DeflateCompressorOutputStream(fileOutputStream);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported compression algorithm: " + algorithm);
            }
            compressorOutputStream.write(getStream());
            compressorOutputStream.flush();
            compressorOutputStream.close();
        } catch (Exception e) {
            throw new StreamBatchException(getClass().getSimpleName(), e.getMessage());
        }
    }
}
