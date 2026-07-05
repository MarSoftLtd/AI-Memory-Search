package com.bliss.aimemorysearch.ai.concept;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.DataInputStream;

public class ConceptBinaryLoader {

    public ConceptEmbeddingDatabase load(Context context) throws Exception {

        ConceptEmbeddingDatabase database =
                new ConceptEmbeddingDatabase();

        DataInputStream input =
                new DataInputStream(
                        new BufferedInputStream(
                                context.getAssets().open(
                                        "concepts/concepts.bin"
                                )
                        )
                );

        int version =
                Integer.reverseBytes(
                        input.readInt()
                );

        int count =
                Integer.reverseBytes(
                        input.readInt()
                );

        android.util.Log.d(
                "CONCEPT_DB",
                "Version = " + version
        );

        android.util.Log.d(
                "CONCEPT_DB",
                "Concepts = " + count
        );

        for (int i = 0; i < count; i++) {

            int idLength =
                    Short.reverseBytes(
                            input.readShort()
                    ) & 0xffff;

            byte[] idBytes =
                    new byte[idLength];

            input.readFully(idBytes);

            String id =
                    new String(
                            idBytes,
                            "UTF-8"
                    );

            int englishLength =
                    Short.reverseBytes(
                            input.readShort()
                    ) & 0xffff;

            byte[] englishBytes =
                    new byte[englishLength];

            input.readFully(englishBytes);

            String english =
                    new String(
                            englishBytes,
                            "UTF-8"
                    );

            int type =
                    input.readUnsignedByte();

            int dimension =
                    Integer.reverseBytes(
                            input.readInt()
                    );

            float[] embedding =
                    new float[dimension];

            for (int d = 0; d < dimension; d++) {

                int bits =
                        Integer.reverseBytes(
                                input.readInt()
                        );

                embedding[d] =
                        Float.intBitsToFloat(bits);
            }

            database.add(

                    new ConceptEmbedding(

                            id,

                            english,

                            ConceptType.values()[type],

                            embedding
                    )
            );
        }

        input.close();

        return database;
    }
}