package com.group8.comp2300.routes

import com.group8.comp2300.dto.ErrorResponse
import com.group8.comp2300.dto.MessageResponse
import com.group8.comp2300.dto.RenameRequest
import com.group8.comp2300.dto.toDto
import com.group8.comp2300.service.medicalRecords.MedicalRecordService
import com.group8.comp2300.service.medicalRecords.UploadResult
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
    val service by inject<MedicalRecordService>()

    authenticate("auth-jwt") {
        route("/api/medical-records") {
            post("/upload") {
                withUserId { userId ->
                    val multipart = call.receiveMultipart()
                    var result: UploadResult? = null
                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem && result == null) {
                            result = service.uploadMedicalRecord(userId, part)
                        }
                        part.dispose()
                    }
                    when (val r = result) {
                        is UploadResult.Success -> call.respond(
                            HttpStatusCode.Created,
                            MessageResponse("File uploaded successfully"),
                        )

                        is UploadResult.Failed -> call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(r.reason),
                        )

                        null -> call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("No file provided"),
                        )
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
                    var result: UploadResult? = null

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem && result == null) {
                            val fileName = part.originalFileName ?: "updated_file"
                            val fileBytes = part.provider().readRemaining().readByteArray()
                            result = service.reuploadRecord(id, userId, fileName, fileBytes)
                        }
                        part.dispose()
                    }

                    when (val r = result) {
                        is UploadResult.Success -> call.respond(
                            HttpStatusCode.OK,
                            MessageResponse("File updated successfully"),
                        )

                        is UploadResult.Failed -> call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(r.reason),
                        )

                        null -> call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("No file provided"),
                        )
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
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("File not found"))
                    }
                }
            }

            patch("/rename/{id}") {
                val id = call.parameters["id"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
                withUserId { userId ->
                    val request = call.receiveNullable<RenameRequest>()
                        ?: return@withUserId call.respond(HttpStatusCode.BadRequest)

                    if (service.renameRecord(id, userId, request.newName)) {
                        call.respond(HttpStatusCode.OK, MessageResponse("Renamed successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Record not found"))
                    }
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                withUserId { userId ->
                    if (service.deleteRecord(id, userId)) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Record not found or delete failed"))
                    }
                }
            }
        }
    }
}
