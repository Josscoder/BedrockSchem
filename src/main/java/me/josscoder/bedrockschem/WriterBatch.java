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

        int maxX = (int) boundingBox.getMaxX(),
                maxY = (int) boundingBox.getMaxY(),
                maxZ = (int) boundingBox.getMaxZ();

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        Block[][][] grid = new Block[sizeX][sizeY][sizeZ];

        boundingBox.forEach((x, y, z) -> {
            int blockId = level.getBlockIdAt(x, y, z);
            if (blockId == Block.AIR) return;
            int blockData = level.getBlockDataAt(x, y, z);

            grid[x-minX][y-minY][z-minZ] = Block.get(blockId, blockData);
        });

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Block blockAt = grid[x][y][z];

                    blocksStream.putInt(blockAt.getId());
                    blocksStream.putInt(blockAt.getDamage());

                    blocksStream.putInt(x);
                    blocksStream.putInt(y);
                    blocksStream.putInt(z);

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
