package me.josscoder.bedrockschem;

import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.Zlib;
import com.github.luben.zstd.Zstd;
import lombok.Getter;
import org.xerial.snappy.Snappy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

    public void saveAsFile(File file) throws StreamBatchException, IOException {
        saveAsFile(file, CompressorType.ZSTD);
    }

    public void saveAsFile(File file, CompressorType compressorType) throws StreamBatchException, IOException {
        if (!file.exists()) file.createNewFile();

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] compressedData = new byte[]{};

            System.out.println("compressed data antes " + compressedData.length);

            switch (compressorType) {
                case ZSTD:
                    compressedData = Zstd.compress(getStream(), 7);
                    break;
                case ZLIB:
                    compressedData = Zlib.deflate(getStream(), 7);
                    break;
                case SNAPPY:
                    compressedData = Snappy.compress(getStream());
                    break;
            }
            fileOutputStream.write(compressedData);
            fileOutputStream.close();

            System.out.println("compressed data despues " + compressedData.length);
        } catch (Exception e) {
            throw new StreamBatchException(getClass().getSimpleName(), e.getMessage());
        }
    }
}
