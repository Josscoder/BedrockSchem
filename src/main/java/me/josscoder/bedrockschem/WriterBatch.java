package me.josscoder.bedrockschem;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.snappy.SnappyCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriterBatch {

    @Getter
    private int countBlocks = 0;
    private final BinaryStream blocksStream = new BinaryStream();

    public void addBlocksFrom3DPolygon(AxisAlignedBB boundingBox, Level level) {
        int minX = (int) boundingBox.getMinX(),
                minY = (int) boundingBox.getMinY(),
                minZ = (int) boundingBox.getMinZ();

        boundingBox.forEach((x, y, z) -> {
            int blockId = level.getBlockIdAt(x, y, z);
            if (blockId == Block.AIR) return;
            int blockData = level.getBlockDataAt(x, y, z);

            blocksStream.putInt(blockId);
            blocksStream.putInt(blockData);

            blocksStream.putInt(x - minX);
            blocksStream.putInt(y - minY);
            blocksStream.putInt(z - minZ);

            countBlocks++;
        });
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
