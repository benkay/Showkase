package com.airbnb.showkase.processor.writer

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import javax.annotation.processing.ProcessingEnvironment
import com.squareup.kotlinpoet.TypeSpec
import com.airbnb.showkase.annotation.models.ShowkaseCodegenMetadata
import com.airbnb.showkase.processor.ShowkaseProcessor.Companion.CODEGEN_PACKAGE_NAME
import com.airbnb.showkase.processor.models.ShowkaseMetadata
import com.airbnb.showkase.processor.models.ShowkaseMetadataType
import javax.lang.model.util.Types

internal class ShowkaseCodegenMetadataWriter(private val processingEnv: ProcessingEnvironment) {

    internal fun generateShowkaseCodegenFunctions(
        showkaseMetadataSet: Set<ShowkaseMetadata>,
        typeUtil: Types
    ) {
        if (showkaseMetadataSet.isEmpty()) return
        val moduleName = showkaseMetadataSet.first().packageSimpleName
        val generatedClassName = "ShowkaseMetadata${moduleName.capitalize()}"
        val fileBuilder = FileSpec.builder(
            CODEGEN_PACKAGE_NAME,
            generatedClassName
        )
            .addComment("This is an auto-generated file. Please do not edit/modify this file.")

        val autogenClass = TypeSpec.classBuilder(generatedClassName)

        showkaseMetadataSet.forEach { showkaseMetadata ->
            val methodName = when {
                showkaseMetadata.enclosingClass == null -> showkaseMetadata.elementName
                else -> {
                    val enclosingClassName =
                        typeUtil.asElement(showkaseMetadata.enclosingClass).simpleName
                    "${enclosingClassName}_${showkaseMetadata.elementName}"
                }
            }

            val annotation = AnnotationSpec.builder(ShowkaseCodegenMetadata::class)
                .addMember("showkaseName = %S", showkaseMetadata.showkaseName)
                .addMember("showkaseGroup = %S", showkaseMetadata.showkaseGroup)
                .addMember("packageName = %S", showkaseMetadata.packageName)
                .addMember("packageSimpleName = %S", showkaseMetadata.packageSimpleName)
                .addMember("showkaseElementName = %S", showkaseMetadata.elementName)
                .addMember("insideObject = ${showkaseMetadata.insideObject}")
                .addMember("insideWrapperClass = ${showkaseMetadata.insideWrapperClass}")
                .addMember("showkaseKDoc = %S", showkaseMetadata.showkaseKDoc)
            showkaseMetadata.enclosingClass?.let {
                annotation.addMember("enclosingClass = [%T::class]", it)
            }
            when (showkaseMetadata) {
                is ShowkaseMetadata.Component -> {
                    annotation.apply {
                        addMember("showkaseMetadataType = %S", ShowkaseMetadataType.COMPONENT.name)
                        showkaseMetadata.showkaseWidthDp?.let {
                            addMember("showkaseWidthDp = %L", it)
                        }
                        showkaseMetadata.showkaseHeightDp?.let { 
                            addMember("showkaseHeightDp = %L", it)
                        }
                    }
                }
                is ShowkaseMetadata.Color -> {
                    annotation.addMember("showkaseMetadataType = %S", ShowkaseMetadataType.COLOR.name)
                }
            }
            autogenClass.addFunction(
                FunSpec.builder(methodName)
                    .addAnnotation(annotation.build())
                    .build()
            )
        }

        fileBuilder.addType(
            with(autogenClass) {
                showkaseMetadataSet.forEach { addOriginatingElement(it.element) }
                build()
            }
        )

        fileBuilder.build().writeTo(processingEnv.filer)
    }
}
