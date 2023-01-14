package me.josscoder.bedrockschem;

import cn.nukkit.block.Block;
import cn.nukkit.level.Position;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.utils.BinaryStream;
import lombok.Getter;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.snappy.SnappyCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ReaderBatch {

    @Getter
    private int countBlocks;
    private final BinaryStream stream;

    public ReaderBatch(byte[] bytes) throws StreamBatchException {
        try {
            stream = new BinaryStream(bytes);
            countBlocks = stream.getInt();
        } catch (Exception e) {
            throw new StreamBatchException(getClass().getSimpleName(), e.getMessage());
        }
    }

    public static ReaderBatch makeFromFile(File file) throws StreamBatchException {
        return makeFromFile(file, CompressionAlgorithm.ZSTD);
    }

    public static ReaderBatch makeFromFile(File file, CompressionAlgorithm algorithm) throws StreamBatchException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            CompressorInputStream compressorInputStream;
            switch (algorithm) {
                case ZSTD:
                    compressorInputStream = new ZstdCompressorInputStream(fileInputStream);
                    break;
                case SNAPPY:
                    compressorInputStream = new SnappyCompressorInputStream(fileInputStream);
                    break;
                case ZLIB:
                    compressorInputStream = new DeflateCompressorInputStream(fileInputStream);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported compression algorithm: " + algorithm);
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = compressorInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, read);
            }
            compressorInputStream.close();
            return new ReaderBatch(byteArrayOutputStream.toByteArray());
        } catch (IOException | StreamBatchException e) {
            throw new StreamBatchException(ReaderBatch.class.getSimpleName(), e.getMessage());
        }
    }

    public Block readNextBlock() {
        return Block.get(stream.getInt(), stream.getInt());
    }

    public void buildAt(Position position) throws StreamBatchException {
        buildAt(position, null);
    }

    public void buildAt(Position position, AxisAlignedBB.BBConsumer action) throws StreamBatchException {
        try {
            for (int i = 0; i < countBlocks; i++) {
                Block block = readNextBlock();
                if (block == null) continue;

                int x = stream.getInt();
                int y = stream.getInt();
                int z = stream.getInt();

                int floorX = position.getFloorX();
                int floorY = position.getFloorY();
                int floorZ = position.getFloorZ();

                position.getLevel().setBlockAt((floorX + x),
                        (floorY + y),
                        (floorZ + z),
                        block.getId(),
                        block.getDamage()
                );

                if (action != null) action.accept((floorX + x), (floorY + y), (floorZ + z));
            }
        } catch (Exception e) {
            throw new StreamBatchException(getClass().getSimpleName(), e.getMessage());
        }
    }
}
