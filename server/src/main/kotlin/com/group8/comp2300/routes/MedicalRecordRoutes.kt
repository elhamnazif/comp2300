package com.group8.comp2300.routes

import com.group8.comp2300.dto.ApiResponse
import com.group8.comp2300.dto.RenameRequest
import com.group8.comp2300.service.medicalRecords.MedicalRecordService
import com.group8.comp2300.service.medicalRecords.toDto
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get


fun Route.medicalRecordRoutes() {
    val service = get<MedicalRecordService>()

    route("/api/medical-records") {

        post("/upload") {
            // 1. The HTTP-level check (Fast reject)
            val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLong() ?: 0L
            val maxFileSize = 10 * 1024 * 1024L // 10 MB

            if (contentLength > maxFileSize) {
                call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    ApiResponse(false, "File exceeds the 10MB limit.")
                )
                return@post
            }

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
                    call.respond(HttpStatusCode.Created, ApiResponse(true, "File uploaded successfully"))
                } else {
                    // If the service rejected it, it returns null
                    call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Upload failed or file was invalid"))
                }
            }
        }

        get("/user") {
            withUserId { userId ->
                val records = service.getRecordsForUser(userId).map { it.toDto() }
                call.respond(HttpStatusCode.OK, records)
            }
        }

        get("/download/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            withUserId { userId ->
                val file = service.getPhysicalFile(id, userId)
                if (file != null && file.exists()) {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, file.name).toString()
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
                    ?: return@withUserId call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid request"))

                if (service.renameRecord(id, userId, request.newName)) {
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Renamed successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Record not found"))
                }
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            withUserId { userId ->
                if (service.deleteRecord(id, userId)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Delete failed"))
                }
            }
        }

        get("/api/medical-records/user") {
            val userId = "user-123" // Replace with actual JWT extraction logic

            // Grab the "?sort=" parameter from the URL, or null if it doesn't exist
            val sortParam = call.request.queryParameters["sort"]

            val records = service.getRecordsForUser(userId, sortParam)
            call.respond(HttpStatusCode.OK, records)
        }

        put("/api/medical-records/{id}/reupload") {
            val userId = "user-123" // Replace with actual JWT logic
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)

            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null
            var fileName: String? = null

            // Parse the incoming file
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    fileName = part.originalFileName
                    fileBytes = part.streamProvider().readBytes()
                }
                part.dispose()
            }

            if (fileBytes != null && fileName != null) {
                val success = service.reuploadRecord(id, userId, fileName!!, fileBytes!!)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "File successfully reuploaded"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Record not found or user unauthorized"))
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing file data"))
            }
        }
    }
}
