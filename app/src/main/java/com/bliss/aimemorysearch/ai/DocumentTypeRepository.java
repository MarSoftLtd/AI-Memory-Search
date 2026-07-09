package com.bliss.aimemorysearch.ai;

import java.util.List;

public interface DocumentTypeRepository {

    DocumentTypeDefinition findDocumentTypeById(
            String documentTypeId
    );

    List<DocumentTypeDefinition> getAllDocumentTypes();
}
