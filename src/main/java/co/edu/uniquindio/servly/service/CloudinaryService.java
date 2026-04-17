package co.edu.uniquindio.servly.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Sube una imagen a Cloudinary y retorna la URL
     * @param file Archivo de imagen
     * @param folder Carpeta donde guardar (ej: "products", "recipes")
     * @return Mapa con imageUrl y publicId
     */
    public Map<String, String> uploadImage(MultipartFile file, String folder) {
        try {
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", folder);
            uploadParams.put("resource_type", "auto");

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            return Map.of(
                    "imageUrl", (String) uploadResult.get("secure_url"),
                    "publicId", (String) uploadResult.get("public_id")
            );
        } catch (IOException e) {
            log.error("Error al subir imagen a Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Error al subir imagen", e);
        }
    }

    /**
     * Elimina una imagen de Cloudinary por su publicId
     * @param publicId ID público de la imagen
     */
    public void deleteImage(String publicId) {
        try {
            if (publicId != null && !publicId.isEmpty()) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Imagen eliminada de Cloudinary: {}", publicId);
            }
        } catch (IOException e) {
            log.error("Error al eliminar imagen de Cloudinary: {}", e.getMessage());
        }
    }
}

