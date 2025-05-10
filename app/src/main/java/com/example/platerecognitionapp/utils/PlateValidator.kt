package com.example.platerecognitionapp.utils

object PlateValidator {
    private val PORTUGUESE_PLATE_REGEX = Regex("^[A-Z]{2}-[0-9]{2}-[A-Z]{2}$")

    fun isValidPortuguesePlate(plate: String): Boolean {
        return PORTUGUESE_PLATE_REGEX.matches(plate)
    }

    fun formatPlate(plate: String): String? {
        // Remove espaços e hífens
        val cleanPlate = plate.replace(Regex("[\\s-]"), "")
        
        // Verifica se tem 6 caracteres
        if (cleanPlate.length != 6) return null

        // Formata para XX-00-XX
        return "${cleanPlate.substring(0, 2)}-${cleanPlate.substring(2, 4)}-${cleanPlate.substring(4)}"
    }
}
