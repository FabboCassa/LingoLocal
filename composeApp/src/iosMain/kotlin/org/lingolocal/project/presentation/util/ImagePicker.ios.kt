package org.lingolocal.project.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSData
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraDevice
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberImagePicker(
    onImagePicked: (ByteArray?) -> Unit,
    onPermissionDenied: (String) -> Unit
): ImagePickerLauncher {
    return remember(onImagePicked, onPermissionDenied) {
        IosImagePickerLauncher(onImagePicked, onPermissionDenied)
    }
}

private class IosImagePickerLauncher(
    private val onImagePicked: (ByteArray?) -> Unit,
    private val onPermissionDenied: (String) -> Unit
) : ImagePickerLauncher {

    // Riferimento forte per evitare che il delegato venga deallocato dal Garbage Collector durante la selezione
    private var activeDelegate: ImagePickerDelegate? = null

    override fun launchGallery() {
        val rootVc = getRootViewController()
        if (rootVc == null) {
            onPermissionDenied("Impossibile trovare il Root ViewController di iOS")
            return
        }

        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)) {
            onPermissionDenied("Galleria fotografica non disponibile su questo dispositivo")
            return
        }

        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        
        val delegate = ImagePickerDelegate(
            onImagePicked = { bytes ->
                onImagePicked(bytes)
                activeDelegate = null // Rilascia il delegato
            },
            onCancel = {
                onImagePicked(null)
                activeDelegate = null // Rilascia il delegato
            }
        )
        activeDelegate = delegate
        picker.delegate = delegate

        rootVc.presentViewController(picker, animated = true, completion = null)
    }

    override fun launchCamera() {
        val rootVc = getRootViewController()
        if (rootVc == null) {
            onPermissionDenied("Impossibile trovare il Root ViewController di iOS")
            return
        }

        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
            onPermissionDenied("Fotocamera non disponibile su questo dispositivo (es. simulatore)")
            return
        }

        val authStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        when (authStatus) {
            AVAuthorizationStatusAuthorized -> {
                presentCamera(rootVc)
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (granted) {
                            presentCamera(rootVc)
                        } else {
                            onPermissionDenied("Permesso fotocamera negato")
                        }
                    }
                }
            }
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
                onPermissionDenied("Il permesso della fotocamera è stato negato nelle impostazioni di sistema")
            }
            else -> {
                onPermissionDenied("Stato di autorizzazione sconosciuto")
            }
        }
    }

    private fun presentCamera(rootVc: UIViewController) {
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.cameraDevice = UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear
        
        val delegate = ImagePickerDelegate(
            onImagePicked = { bytes ->
                onImagePicked(bytes)
                activeDelegate = null // Rilascia il delegato
            },
            onCancel = {
                onImagePicked(null)
                activeDelegate = null // Rilascia il delegato
            }
        )
        activeDelegate = delegate
        picker.delegate = delegate

        rootVc.presentViewController(picker, animated = true, completion = null)
    }

    private fun getRootViewController(): UIViewController? {
        val keyWindow = UIApplication.sharedApplication.keyWindow 
            ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        return keyWindow?.rootViewController
    }
}

private class ImagePickerDelegate(
    private val onImagePicked: (ByteArray?) -> Unit,
    private val onCancel: () -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    @OptIn(BetaInteropApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage)
            ?: (didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage)

        if (image != null) {
            // Comprime l'immagine scattata/selezionata in JPEG con qualità 0.8
            val jpegData = UIImageJPEGRepresentation(image, 0.8)
            if (jpegData != null) {
                val bytes = jpegData.toByteArray()
                onImagePicked(bytes)
            } else {
                onImagePicked(null)
            }
        } else {
            onImagePicked(null)
        }
        picker.dismissViewControllerAnimated(true, null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onCancel()
    }
}

/**
 * Utility per convertire in modo efficiente i dati NSData di iOS in un ByteArray di Kotlin
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return byteArray
}
