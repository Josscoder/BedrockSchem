package me.josscoder.bedrockschem;

import cn.nukkit.block.Block;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.Zlib;
import com.github.luben.zstd.Zstd;
import lombok.Getter;
import org.xerial.snappy.Snappy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Consumer;

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
        return makeFromFile(file, CompressorType.ZSTD);
    }

    public static ReaderBatch makeFromFile(File file, CompressorType compressorType) throws StreamBatchException {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] compressed = new byte[fileInputStream.available()];
            fileInputStream.read(compressed);
            fileInputStream.close();

            byte[] decompressed = new byte[]{};

            System.out.println("decompressed antes " + decompressed.length);

            switch (compressorType) {
                case ZSTD:
                    decompressed = Zstd.decompress(compressed, compressed.length);
                    break;
                case ZLIB:
                    decompressed = Zlib.inflate(compressed);
                    break;
                case SNAPPY:
                    decompressed = Snappy.uncompress(compressed);
                    break;
            }

            System.out.println("decompressed despues " + decompressed.length);

            return new ReaderBatch(decompressed);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                if (block == null || block.getId() == Block.AIR) continue;

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
