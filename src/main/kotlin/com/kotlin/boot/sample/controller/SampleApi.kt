package com.kotlin.boot.sample.controller

import com.kotlin.boot.sample.service.SampleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController("/")
class SampleApi(
        private val sampleService: SampleService
) {

    @PostMapping("/db/post")
    @Deprecated(message = "DEPRECATED", level = DeprecationLevel.HIDDEN)
    fun postTest(
            @RequestParam userId: String
    ) {
        sampleService.saveData(userId)
    }

    @GetMapping("/db/get")
    @Deprecated(message = "DEPRECATED", level = DeprecationLevel.HIDDEN)
    fun getTest(
            @RequestParam userId: String
    ) = sampleService.findData(userId)
}