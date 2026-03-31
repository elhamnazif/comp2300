package com.group8.comp2300.routes

import com.group8.comp2300.dto.ApiResponse
import com.group8.comp2300.dto.RenameRequest
import com.group8.comp2300.service.medicalRecords.MedicalRecordService
import com.group8.comp2300.service.medicalRecords.toDto
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.koin.ktor.ext.inject

fun Route.medicalRecordRoutes() {
    // Use inject() so it lazily resolves from the Koin context we set up in the test
    val service by inject<MedicalRecordService>()

    authenticate("auth-jwt") {
        // Removed the extra "/api/medical-records" nesting here
        // because it's usually handled in the main Application.kt or the Test setup
        route("/api/medical-records") {
            post("/upload") {
                withUserId { userId ->
                    val multipart = call.receiveMultipart()
                    var success = false
                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            val record = service.uploadMedicalRecord(userId, part)
                            if (record != null) success = true
                        }
                        part.dispose()
                    }
                    if (success) {
                        call.respond(HttpStatusCode.Created, ApiResponse(true, "Success"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Failed"))
                    }
                }
            }

            get("/user") {
                withUserId { userId ->
                    val sortParam = call.request.queryParameters["sort"]
                    val records = service.getRecordsForUser(userId, sortParam).map { it.toDto() }
                    call.respond(HttpStatusCode.OK, records)
                }
            }

            put("/reupload/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)

                withUserId { userId ->
                    val multipart = call.receiveMultipart()
                    var success = false

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            val fileName = part.originalFileName ?: "updated_file"
                            // Read the file stream into a ByteArray for the service
                            val fileBytes = part.provider().readRemaining().readByteArray()

                            success = service.reuploadRecord(id, userId, fileName, fileBytes)
                        }
                        part.dispose()
                    }

                    if (success) {
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "File updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, "Failed to update file"))
                    }
                }
            }

            get("/download/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                withUserId { userId ->
                    val file = service.getPhysicalFile(id, userId)
                    if (file?.exists() == true) {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Inline.withParameter(
                                ContentDisposition.Parameters.FileName,
                                file.name,
                            ).toString(),
                        )
                        call.respondFile(file)
                    } else {
                        call.respond(HttpStatusCode.NotFound, ApiResponse(false, "File not found"))
                    }
                }
            }

            patch("/rename/{id}") {
                val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                withUserId { userId ->
                    val request = call.receiveNullable<RenameRequest>()
                        ?: return@withUserId call.respond(HttpStatusCode.BadRequest)

                    if (service.renameRecord(id, userId, request.newName)) {
                        call.respond(HttpStatusCode.OK, ApiResponse(true, "Renamed"))
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                withUserId { userId ->
                    if (service.deleteRecord(id, userId)) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        // This returns 404 if the record doesn't exist or belongs to someone else
                        call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Delete failed"))
                    }
                }
            }
        }
    }
}
