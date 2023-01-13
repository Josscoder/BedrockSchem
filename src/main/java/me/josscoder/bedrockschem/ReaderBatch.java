package me.josscoder.bedrockschem;

import cn.nukkit.block.Block;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.BinaryStream;
import com.github.luben.zstd.ZstdInputStream;
import lombok.Getter;
import org.xerial.snappy.SnappyInputStream;

import java.io.*;
import java.util.function.Consumer;
import java.util.zip.InflaterInputStream;

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
            InputStream inputStream;
            switch (algorithm) {
                case ZSTD:
                    inputStream = new ZstdInputStream(fileInputStream);
                    break;
                case SNAPPY:
                    inputStream = new SnappyInputStream(fileInputStream);
                    break;
                case ZLIB:
                    inputStream = new InflaterInputStream(fileInputStream);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported compression algorithm: " + algorithm);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return new ReaderBatch(baos.toByteArray());
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

    public void buildAt(Position position, Consumer<Vector3> callback) throws StreamBatchException {
        try {
            for (int i = 0; i < countBlocks; i++) {
                Block block = readNextBlock();
                if (block == null) continue;

                int x = (int) (position.getX() + stream.getInt());
                int y = (int) (position.getY() + stream.getInt());
                int z = (int) (position.getZ() + stream.getInt());

                position.getLevel().setBlockAt(x, y, z, block.getId(), block.getDamage());
                if (callback != null) callback.accept(new Vector3(x, y, z));
            }
        } catch (Exception e) {
            throw new StreamBatchException(getClass().getSimpleName(), e.getMessage());
        }
    }
}
