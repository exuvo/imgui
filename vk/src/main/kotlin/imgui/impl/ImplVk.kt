package imgui.impl
//
//import glm_.vec2.Vec2
//import glm_.vec2.Vec2i
//import imgui.internal.DrawVert
//import kool.adr
//import kool.isValid
//import kool.toBuffer
//import org.lwjgl.system.MemoryUtil.NULL
//import org.lwjgl.vulkan.*
//import org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL
//import vkk.*
//import vkk.entities.*
//import vkk.extensionFunctions.*
//
//
//// Initialization data, for ImGui_ImplVulkan_Init()
//// [Please zero-clear before use!]
//object VkInitInfo {
//    lateinit var instance: VkInstance
//    lateinit var physicalDevice: VkPhysicalDevice
//    lateinit var device: VkDevice
//    var queueFamily = 0
//    lateinit var queue: VkQueue
//    var pipelineCache = VkPipelineCache()
//    var descriptorPool = VkDescriptorPool()
//    var minImageCount = 0 // >= 2
//    var msaaSamples: VkSampleCount = VkSampleCount(0)   // >= VK_SAMPLE_COUNT_1_BIT
//    var imageCount = 0 // >= MinImageCount
//}
//
//// Called by user code -> ImplVk_userCode
////IMGUI_IMPL_API bool     ImGui_ImplVulkan_Init(ImGui_ImplVulkan_InitInfo* info, VkRenderPass render_pass);
////IMGUI_IMPL_API void     ImGui_ImplVulkan_Shutdown();
////IMGUI_IMPL_API void     ImGui_ImplVulkan_NewFrame();
////IMGUI_IMPL_API void     ImGui_ImplVulkan_RenderDrawData(ImDrawData* draw_data, VkCommandBuffer command_buffer);
////IMGUI_IMPL_API bool     ImGui_ImplVulkan_CreateFontsTexture(VkCommandBuffer command_buffer);
////IMGUI_IMPL_API void     ImGui_ImplVulkan_DestroyFontUploadObjects();
////IMGUI_IMPL_API void     ImGui_ImplVulkan_SetMinImageCount(uint32_t min_image_count); // To override MinImageCount after initialization (e.g. if swap chain is recreated)
//
////-------------------------------------------------------------------------
//// Internal / Miscellaneous Vulkan Helpers
//// (Used by example's main.cpp. Used by multi-viewport features. PROBABLY NOT used by your own engine/app.)
////-------------------------------------------------------------------------
//// You probably do NOT need to use or care about those functions.
//// Those functions only exist because:
////   1) they facilitate the readability and maintenance of the multiple main.cpp examples files.
////   2) the upcoming multi-viewport feature will need them internally.
//// Generally we avoid exposing any kind of superfluous high-level helpers in the bindings,
//// but it is too much code to duplicate everywhere so we exceptionally expose them.
////
//// Your engine/app will likely _already_ have code to setup all that stuff (swap chain, render pass, frame buffers, etc.).
//// You may read this code to learn about Vulkan, but it is recommended you use you own custom tailored code to do equivalent work.
//// (The ImGui_ImplVulkanH_XXX functions do not interact with any of the state used by the regular ImGui_ImplVulkan_XXX functions)
////-------------------------------------------------------------------------
//
////struct ImGui_ImplVulkanH_Frame
////struct ImGui_ImplVulkanH_Window
//
//// Helpers -> ImplVk_helpers
////IMGUI_IMPL_API void                 ImGui_ImplVulkanH_CreateWindow(VkInstance instance, VkPhysicalDevice physical_device, VkDevice device, ImGui_ImplVulkanH_Window* wnd, uint32_t queue_family, const VkAllocationCallbacks* allocator, int w, int h, uint32_t min_image_count);
////IMGUI_IMPL_API void                 ImGui_ImplVulkanH_DestroyWindow(VkInstance instance, VkDevice device, ImGui_ImplVulkanH_Window* wnd, const VkAllocationCallbacks* allocator);
////IMGUI_IMPL_API VkSurfaceFormatKHR   ImGui_ImplVulkanH_SelectSurfaceFormat(VkPhysicalDevice physical_device, VkSurfaceKHR surface, const VkFormat* request_formats, int request_formats_count, VkColorSpaceKHR request_color_space);
////IMGUI_IMPL_API VkPresentModeKHR     ImGui_ImplVulkanH_SelectPresentMode(VkPhysicalDevice physical_device, VkSurfaceKHR surface, const VkPresentModeKHR* request_modes, int request_modes_count);
////IMGUI_IMPL_API int                  ImGui_ImplVulkanH_GetMinImageCountFromPresentMode(VkPresentModeKHR present_mode);
//
///** Helper structure to hold the data needed by one rendering frame
// *  (Used by example's main.cpp. Used by multi-viewport features. Probably NOT used by your own engine/app.)
// *  [Please zero-clear before use!] */
//class ImplVkH_Frame {
//    var commandPool = VkCommandPool()
//    var commandBuffer: VkCommandBuffer? = null
//    var fence = VkFence()
//    var backbuffer = VkImage()
//    var backbufferView = VkImageView()
//    var framebuffer = VkFramebuffer()
//}
//
//class VkFrameSemaphores {
//    var imageAcquiredSemaphore = VkSemaphore()
//    var renderCompleteSemaphore = VkSemaphore()
//}
//
///** Helper structure to hold the data needed by one rendering context into one OS window
// *  (Used by example's main.cpp. Used by multi-viewport features. Probably NOT used by your own engine/app.)
// *  [JVM] we can use an object */
//object ImplVkH_Window {
//    val size = Vec2i()
//    var swapchain = VkSwapchainKHR()
//    var surface = VkSurfaceKHR()
//    var surfaceFormat = VkSurfaceFormatKHR.calloc()
//    var presentMode = VkPresentModeKHR(0x7FFFFFFF)
//    var renderPass = VkRenderPass()
//    var clearEnable = true
//    var clearValue = VkClearValue.calloc()
//    /** Current frame being rendered to (0 <= FrameIndex < FrameInFlightCount) */
//    var frameIndex = 0
//    /** Number of simultaneous in-flight frames (returned by vkGetSwapchainImagesKHR, usually derived from min_image_count) */
//    var imageCount = 0
//    /** Current set of swapchain wait semaphores we're using (needs to be distinct from per frame data) */
//    var semaphoreIndex = 0
//    var frames = emptyArray<ImplVkH_Frame>()
//    var frameSemaphores = emptyArray<VkFrameSemaphores>()
//
//    fun reset() {
//        size put 0f
//        swapchain = VkSwapchainKHR()
//        surface = VkSurfaceKHR()
//        surfaceFormat.free()
//        surfaceFormat = VkSurfaceFormatKHR.calloc()
//        presentMode = VkPresentModeKHR(0x7FFFFFFF)
//        renderPass = VkRenderPass()
//        clearEnable = true
//        clearValue.free()
//        clearValue = VkClearValue.calloc()
//        /** Current frame being rendered to (0 <= FrameIndex < FrameInFlightCount) */
//        frameIndex = 0
//        /** Number of simultaneous in-flight frames (returned by vkGetSwapchainImagesKHR, usually derived from min_image_count) */
//        imageCount = 0
//        /** Current set of swapchain wait semaphores we're using (needs to be distinct from per frame data) */
//        semaphoreIndex = 0
//        frames = emptyArray()
//        frameSemaphores = emptyArray()
//    }
//}
//
//// --------------------------------- imgui_impl_vulkan.cpp ---------------------------------
//
//// Reusable buffers used for rendering 1 current in-flight frame, for ImGui_ImplVulkan_RenderDrawData()
//// [Please zero-clear before use!]
//class ImplVk_FrameRenderBuffers {
//    var vertexBufferMemory = VkDeviceMemory.NULL
//    var indexBufferMemory = VkDeviceMemory.NULL
//    var vertexBufferSize = VkDeviceSize.NULL
//    var indexBufferSize = VkDeviceSize.NULL
//    var vertexBuffer = VkBuffer.NULL
//    var indexBuffer = VkBuffer.NULL
//}
//
//// Vulkan data
////static ImGui_ImplVulkan_InitInfo g_VulkanInitInfo = {} // we use an object instead
//var renderPass = VkRenderPass()
//var bufferMemoryAlignment = VkDeviceSize(256)
//var pipelineCreateFlags: VkPipelineCreateFlags = 0x00
//var descriptorSetLayout = VkDescriptorSetLayout.NULL
//var pipelineLayout = VkPipelineLayout.NULL
//var descriptorSet = VkDescriptorSet.NULL
//var pipeline = VkPipeline.NULL
//
//// Font data
//var fontSampler = VkSampler.NULL
//var fontMemory = VkDeviceMemory.NULL
//var fontImage = VkImage.NULL
//var fontView = VkImageView.NULL
//var uploadBufferMemory = VkDeviceMemory.NULL
//var uploadBuffer = VkBuffer.NULL
//
///** Render buffers
// *
// *  Each viewport will hold 1 ImGui_ImplVulkanH_WindowRenderBuffers
// *  [Please zero-clear before use!] */
//object ImplVkH_WindowRenderBuffers {
//    var index = 0
//    var frameRenderBuffers = emptyArray<ImplVk_FrameRenderBuffers>()
//}
//
//// Forward Declarations
//fun ImplVk_CreateDeviceObjects(): Boolean {
//
//    val v = VkInitInfo
//    var vertModule = VkShaderModule()
//    var fragModule = VkShaderModule()
//
//    // Create The Shader Modules:
//    stak {
//        val vertInfo = vk.ShaderModuleCreateInfo { code = __glsl_shader_vert_spv.toBuffer(it) }
//        vertModule = v.device createShaderModule vertInfo
//        val fragInfo = vk.ShaderModuleCreateInfo { code = __glsl_shader_frag_spv.toBuffer(it) }
//        fragModule = v.device createShaderModule fragInfo
//    }
//
//    if (fontSampler.isInvalid) {
//        val info = vk.SamplerCreateInfo {
//            minMagFilter = VkFilter.LINEAR
//            mipmapMode = VkSamplerMipmapMode.LINEAR
//            addressModeUVW = VkSamplerAddressMode.REPEAT
//            minMaxLod(-1000f, 1000f)
//            maxAnisotropy = 1f
//        }
//        fontSampler = v.device createSampler info
//    }
//
//    if (descriptorSetLayout.isInvalid) {
//        val binding = vk.DescriptorSetLayoutBinding {
//            descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER
//            stageFlags = VkShaderStage.FRAGMENT_BIT.i
//            immutableSampler = fontSampler
//        }
//        val info = vk.DescriptorSetLayoutCreateInfo(binding)
//        descriptorSetLayout = v.device createDescriptorSetLayout info
//    }
//
//    // Create Descriptor Set:
//    run {
//        val allocInfo = vk.DescriptorSetAllocateInfo(v.descriptorPool, descriptorSetLayout)
//        descriptorSet = v.device allocateDescriptorSets allocInfo
//    }
//
//    if (pipelineLayout.isInvalid) {
//        // Constants: we are using 'vec2 offset' and 'vec2 scale' instead of a full 3d projection matrix
//        val pushConstants = vk.PushConstantRange(VkShaderStage.VERTEX_BIT.i, Vec2.size * 2)
//        val layoutInfo = vk.PipelineLayoutCreateInfo {
//            setLayout = descriptorSetLayout
//            pushConstantRange = pushConstants
//        }
//        pipelineLayout = v.device createPipelineLayout layoutInfo
//    }
//
//    val stage = vk.PipelineShaderStageCreateInfo(2).also {
//        it[0].apply {
//            stage = VkShaderStage.VERTEX_BIT
//            module = vertModule
//            name = "main"
//        }
//        it[1].apply {
//            stage = VkShaderStage.FRAGMENT_BIT
//            module = fragModule
//            name = "main"
//        }
//    }
//    val bindingDesc = vk.VertexInputBindingDescription {
//        stride = DrawVert.size
//        inputRate = VkVertexInputRate.VERTEX
//    }
//    val attributeDesc = vk.VertexInputAttributeDescription(
//            bindingDesc.binding, 0, VkFormat.R32G32_SFLOAT, 0,
//            bindingDesc.binding, 1, VkFormat.R32G32_SFLOAT, Vec2.size,
//            bindingDesc.binding, 2, VkFormat.R8G8B8A8_UNORM, Vec2.size * 2)
//
//    val vertexInfo = vk.PipelineVertexInputStateCreateInfo {
//        vertexBindingDescription = bindingDesc
//        vertexAttributeDescriptions = attributeDesc
//    }
//    val iaInfo = vk.PipelineInputAssemblyStateCreateInfo(VkPrimitiveTopology.TRIANGLE_LIST)
//
//    val viewportInfo = vk.PipelineViewportStateCreateInfo(viewportCount = 1, scissorCount = 1)
//
//    val rasterInfo = vk.PipelineRasterizationStateCreateInfo {
//        polygonMode = VkPolygonMode.FILL
//        cullMode = VkCullMode.NONE.i
//        frontFace = VkFrontFace.CLOCKWISE
//        lineWidth = 1f
//    }
//    val msInfo = vk.PipelineMultisampleStateCreateInfo{
//        rasterizationSamples = if (v.msaaSamples.i != 0) v.msaaSamples else VkSampleCount._1_BIT
//    }
//
//    val colorAttachment = vk.PipelineColorBlendAttachmentState {
//        blendEnable = true
//        srcColorBlendFactor = VkBlendFactor.SRC_ALPHA
//        dstColorBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA
//        colorBlendOp = VkBlendOp.ADD
//        srcAlphaBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA
//        dstAlphaBlendFactor = VkBlendFactor.ZERO
//        alphaBlendOp = VkBlendOp.ADD
//        colorWriteMask = VkColorComponent.R_BIT or VkColorComponent.G_BIT or VkColorComponent.B_BIT or VkColorComponent.A_BIT
//    }
//    val depthInfo = vk.PipelineDepthStencilStateCreateInfo()
//
//    val blendInfo = vk.PipelineColorBlendStateCreateInfo(colorAttachment)
//
//    val dynamicStates = listOf(VkDynamicState.VIEWPORT, VkDynamicState.SCISSOR)
//    val dynamicState = vk.PipelineDynamicStateCreateInfo(dynamicStates)
//
//    val info = vk.GraphicsPipelineCreateInfo {
//        flags = pipelineCreateFlags
//        stages = stage
//        vertexInputState = vertexInfo
//        inputAssemblyState = iaInfo
//        viewportState = viewportInfo
//        rasterizationState = rasterInfo
//        multisampleState = msInfo
//        depthStencilState = depthInfo
//        colorBlendState = blendInfo
//        this.dynamicState = dynamicState
//        layout = pipelineLayout
//        renderPass = imgui.impl.renderPass
//    }
//    pipeline = v.device.createGraphicsPipelines(v.pipelineCache, info)
//
//    v.device destroyShaderModule vertModule
//    v.device destroyShaderModule fragModule
//
//    return true
//}
//
//fun ImplVk_DestroyDeviceObjects() {
//
//    val v = VkInitInfo
//    ImplVkH_DestroyWindowRenderBuffers(v.device)
//    ImplVk_DestroyFontUploadObjects()
//
//    v.device.apply {
//        if (fontView.isValid) {
//            destroy(fontView)
//            fontView = VkImageView.NULL
//        }
//        if (fontImage.isValid) {
//            destroy(fontImage)
//            fontImage = VkImage.NULL
//        }
//        if (fontMemory.isValid) {
//            free(fontMemory)
//            fontMemory = VkDeviceMemory.NULL
//        }
//        if (fontSampler.isValid) {
//            destroy(fontSampler)
//            fontSampler = VkSampler.NULL
//        }
//        if (descriptorSetLayout.isValid) {
//            destroy(descriptorSetLayout)
//            descriptorSetLayout = VkDescriptorSetLayout.NULL
//        }
//        if (pipelineLayout.isValid) {
//            destroy(pipelineLayout)
//            pipelineLayout = VkPipelineLayout.NULL
//        }
//        if (pipeline.isValid) {
//            destroy(pipeline)
//            pipeline = VkPipeline.NULL
//        }
//    }
//}
//
//fun destroyFrame(device: VkDevice, fd: ImplVkH_Frame) {
//
//    device destroy fd.fence
//    device.freeCommandBuffer(fd.commandPool, fd.commandBuffer!!)
//    device destroy fd.commandPool
//    fd.fence = VkFence()
//    fd.commandBuffer = null
//    fd.commandPool = VkCommandPool()
//
//    device destroy fd.backbufferView
//    device destroy fd.framebuffer
//}
//
//fun destroyFrameSemaphores(device: VkDevice, fsd: VkFrameSemaphores) {
//
//    device destroy fsd.imageAcquiredSemaphore
//    device destroy fsd.renderCompleteSemaphore
//    fsd.renderCompleteSemaphore = VkSemaphore()
//    fsd.imageAcquiredSemaphore = VkSemaphore()
//}
//
//infix fun VkDevice.ImplVkH_DestroyFrameRenderBuffers(buffers: ImplVk_FrameRenderBuffers) {
//    if (buffers.vertexBuffer.isValid) {
//        destroyBuffer(buffers.vertexBuffer)
//        buffers.vertexBuffer = VkBuffer.NULL
//    }
//    if (buffers.vertexBufferMemory.isValid) {
//        freeMemory(buffers.vertexBufferMemory)
//        buffers.vertexBufferMemory = VkDeviceMemory.NULL
//    }
//    if (buffers.indexBuffer.isValid) {
//        destroyBuffer(buffers.indexBuffer)
//        buffers.indexBuffer = VkBuffer.NULL
//    }
//    if (buffers.indexBufferMemory.isValid) {
//        freeMemory(buffers.indexBufferMemory)
//        buffers.indexBufferMemory = VkDeviceMemory.NULL
//    }
//    buffers.vertexBufferSize = VkDeviceSize.NULL
//    buffers.indexBufferSize = VkDeviceSize.NULL
//}
//
//fun ImplVkH_DestroyWindowRenderBuffers(device: VkDevice) {
//
//    val buffers = ImplVkH_WindowRenderBuffers
//
//    buffers.frameRenderBuffers.forEach(device::ImplVkH_DestroyFrameRenderBuffers)
//    buffers.frameRenderBuffers = emptyArray()
//    buffers.index = 0
//}
//
///** Also destroy old swap chain and in-flight frames data, if any. */
//fun createWindowSwapChain(physicalDevice: VkPhysicalDevice, device: VkDevice, size: Vec2i, minImageCount_: Int) {
//
//    val wd = ImplVkH_Window
//
//    val oldSwapchain = wd.swapchain
//    device.waitIdle()
//
//    // We don't use ImGui_ImplVulkanH_DestroyWindow() because we want to preserve the old swapchain to create the new one.
//    // Destroy old Framebuffer
//    repeat(wd.imageCount) {
//        destroyFrame(device, wd.frames[it])
//        destroyFrameSemaphores(device, wd.frameSemaphores[it])
//    }
//    wd.frames = emptyArray()
//    wd.frameSemaphores = emptyArray()
//    wd.imageCount = 0
//    if (wd.renderPass.isValid)
//        device destroy wd.renderPass
//
//    // If min image count was not specified, request different count of images dependent on selected present mode
//    val minImageCount = when {
//        minImageCount_ == 0 -> wd.presentMode.ImplVkH_minImageCount
//        else -> minImageCount_
//    }
//
//    // Create Swapchain
//    run {
//        val info = vk.SwapchainCreateInfoKHR {
//            surface = wd.surface
//            this.minImageCount = minImageCount
//            imageFormat = wd.surfaceFormat.format
//            imageColorSpace = wd.surfaceFormat.colorSpace
//            imageArrayLayers = 1
//            imageUsage = VkImageUsage.COLOR_ATTACHMENT_BIT.i
//            imageSharingMode = VkSharingMode.EXCLUSIVE           // Assume that graphics family == present family
//            preTransform = VkSurfaceTransformKHR.IDENTITY_BIT_KHR
//            compositeAlpha = VkCompositeAlphaKHR.OPAQUE_BIT_KHR
//            presentMode = wd.presentMode
//            clipped = true
//            this.oldSwapchain = oldSwapchain
//        }
//        val cap = physicalDevice getSurfaceCapabilitiesKHR wd.surface
//        if (info.minImageCount < cap.minImageCount)
//            info.minImageCount = cap.minImageCount
//        else if (cap.maxImageCount != 0 && info.minImageCount > cap.maxImageCount)
//            info.minImageCount = cap.maxImageCount
//
//        if (cap.currentExtent.width == -1) {
//            wd.size put size
//            info.imageExtent.size put size
//        } else {
//            wd.size put cap.currentExtent.size
//            info.imageExtent.size put cap.currentExtent.size
//        }
//        wd.swapchain = device createSwapchainKHR info
//        wd.imageCount = device.getSwapchainImagesKHR(wd.swapchain)
//        val backbuffers = VkImage_Buffer(16)
//        assert(wd.imageCount >= minImageCount && wd.imageCount < backbuffers.rem)
//        device.getSwapchainImagesKHR(wd.swapchain, backbuffers)
//
//        assert(wd.frames.isEmpty())
//        wd.frames = Array(wd.imageCount) { ImplVkH_Frame() }
//        wd.frameSemaphores = Array(wd.imageCount) { VkFrameSemaphores() }
//        for (i in 0 until wd.imageCount)
//            wd.frames[i].backbuffer = backbuffers[i]
//    }
//    if (oldSwapchain.isValid)
//        device destroy oldSwapchain
//
//    // Create the Render Pass
//    run {
//        val attachment = vk.AttachmentDescription {
//            format = wd.surfaceFormat.format
//            samples = VkSampleCount._1_BIT
//            loadOp = if (wd.clearEnable) VkAttachmentLoadOp.CLEAR else VkAttachmentLoadOp.DONT_CARE
//            storeOp = VkAttachmentStoreOp.STORE
//            stencilLoadOp = VkAttachmentLoadOp.DONT_CARE
//            stencilStoreOp = VkAttachmentStoreOp.DONT_CARE
//            initialLayout = VkImageLayout.UNDEFINED
//            finalLayout = VkImageLayout.PRESENT_SRC_KHR
//        }
//        val colorAttachment = vk.AttachmentReference {
//            this.attachment = 0
//            layout = VkImageLayout.COLOR_ATTACHMENT_OPTIMAL
//        }
//        val subpass = vk.SubpassDescription {
//            pipelineBindPoint = VkPipelineBindPoint.GRAPHICS
//            colorAttachmentCount = 1
//            this.colorAttachment = colorAttachment
//        }
//        val dependency = vk.SubpassDependency {
//            srcSubpass = VK_SUBPASS_EXTERNAL
//            dstSubpass = 0
//            srcStageMask = VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i
//            dstStageMask = VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i
//            srcAccessMask = 0
//            dstAccessMask = VkAccess.COLOR_ATTACHMENT_WRITE_BIT.i
//        }
//        val info = vk.RenderPassCreateInfo {
//            this.attachment = attachment
//            this.subpass = subpass
//            this.dependency = dependency
//        }
//        wd.renderPass = device createRenderPass info
//    }
//
//    // Create The Image Views
//    run {
//        val info = vk.ImageViewCreateInfo {
//            viewType = VkImageViewType._2D
//            format = wd.surfaceFormat.format
//            components(VkComponentSwizzle.R, VkComponentSwizzle.G, VkComponentSwizzle.B, VkComponentSwizzle.A)
//        }
//        val imageRange = vk.ImageSubresourceRange {
//            aspectMask = VkImageAspect.COLOR_BIT.i
//            baseMipLevel = 0
//            levelCount = 1
//            baseArrayLayer = 0
//            layerCount = 1
//        }
//        info.subresourceRange = imageRange
//        for (fd in wd.frames) {
//            info.image = fd.backbuffer
//            fd.backbufferView = device createImageView info
//        }
//    }
//
//    // Create Framebuffer
//    run {
//        val attachment = vk.ImageView_Buffer(1)
//        val info = vk.FramebufferCreateInfo {
//            renderPass = wd.renderPass
//            attachments = attachment
//            extent(wd.size, 1)
//        }
//        for (fd in wd.frames) {
//            attachment[0] = fd.backbufferView
//            device createFramebuffer info
//        }
//    }
//}
//
//fun createWindowCommandBuffers(physicalDevice: VkPhysicalDevice, device: VkDevice, queueFamily: Int) {
//
//    assert(physicalDevice.adr != NULL && device.isValid)
//
//    val wd = ImplVkH_Window
//
//    // Create Command Buffers
//    for (i in 0 until wd.imageCount) {
//        val fd = wd.frames[i]
//        val fsd = wd.frameSemaphores[i]
//        run {
//            val info = vk.CommandPoolCreateInfo {
//                flags = VkCommandPoolCreate.RESET_COMMAND_BUFFER_BIT.i
//                queueFamilyIndex = queueFamily
//            }
//            fd.commandPool = device createCommandPool info
//        }
//        run {
//            val info = vk.CommandBufferAllocateInfo {
//                commandPool = fd.commandPool
//                level = VkCommandBufferLevel.PRIMARY
//                commandBufferCount = 1
//            }
//            fd.commandBuffer = device allocateCommandBuffers info
//        }
//        run {
//            val info = vk.FenceCreateInfo { flags = VkFenceCreate.SIGNALED_BIT.i }
//            fd.fence = device createFence info
//        }
//        run {
//            val info = vk.SemaphoreCreateInfo()
//            fsd.imageAcquiredSemaphore = device createSemaphore info
//            fsd.renderCompleteSemaphore = device createSemaphore info
//        }
//    }
//}
//
////-----------------------------------------------------------------------------
//// SHADERS
////-----------------------------------------------------------------------------
//
//// glsl_shader.vert, compiled with:
//// # glslangValidator -V -x -o glsl_shader.vert.u32 glsl_shader.vert
///*
//#version 450 core
//layout(location = 0) in vec2 aPos;
//layout(location = 1) in vec2 aUV;
//layout(location = 2) in vec4 aColor;
//layout(push_constant) uniform uPushConstant { vec2 uScale; vec2 uTranslate; } pc;
//
//out gl_PerVertex { vec4 gl_Position; };
//layout(location = 0) out struct { vec4 Color; vec2 UV; } Out;
//
//void main()
//{
//    Out.Color = aColor;
//    Out.UV = aUV;
//    gl_Position = vec4(aPos * pc.uScale + pc.uTranslate, 0, 1);
//}
//*/
//val __glsl_shader_vert_spv = intArrayOf(
//        0x07230203, 0x00010000, 0x00080001, 0x0000002e, 0x00000000, 0x00020011, 0x00000001, 0x0006000b,
//        0x00000001, 0x4c534c47, 0x6474732e, 0x3035342e, 0x00000000, 0x0003000e, 0x00000000, 0x00000001,
//        0x000a000f, 0x00000000, 0x00000004, 0x6e69616d, 0x00000000, 0x0000000b, 0x0000000f, 0x00000015,
//        0x0000001b, 0x0000001c, 0x00030003, 0x00000002, 0x000001c2, 0x00040005, 0x00000004, 0x6e69616d,
//        0x00000000, 0x00030005, 0x00000009, 0x00000000, 0x00050006, 0x00000009, 0x00000000, 0x6f6c6f43,
//        0x00000072, 0x00040006, 0x00000009, 0x00000001, 0x00005655, 0x00030005, 0x0000000b, 0x0074754f,
//        0x00040005, 0x0000000f, 0x6c6f4361, 0x0000726f, 0x00030005, 0x00000015, 0x00565561, 0x00060005,
//        0x00000019, 0x505f6c67, 0x65567265, 0x78657472, 0x00000000, 0x00060006, 0x00000019, 0x00000000,
//        0x505f6c67, 0x7469736f, 0x006e6f69, 0x00030005, 0x0000001b, 0x00000000, 0x00040005, 0x0000001c,
//        0x736f5061, 0x00000000, 0x00060005, 0x0000001e, 0x73755075, 0x6e6f4368, 0x6e617473, 0x00000074,
//        0x00050006, 0x0000001e, 0x00000000, 0x61635375, 0x0000656c, 0x00060006, 0x0000001e, 0x00000001,
//        0x61725475, 0x616c736e, 0x00006574, 0x00030005, 0x00000020, 0x00006370, 0x00040047, 0x0000000b,
//        0x0000001e, 0x00000000, 0x00040047, 0x0000000f, 0x0000001e, 0x00000002, 0x00040047, 0x00000015,
//        0x0000001e, 0x00000001, 0x00050048, 0x00000019, 0x00000000, 0x0000000b, 0x00000000, 0x00030047,
//        0x00000019, 0x00000002, 0x00040047, 0x0000001c, 0x0000001e, 0x00000000, 0x00050048, 0x0000001e,
//        0x00000000, 0x00000023, 0x00000000, 0x00050048, 0x0000001e, 0x00000001, 0x00000023, 0x00000008,
//        0x00030047, 0x0000001e, 0x00000002, 0x00020013, 0x00000002, 0x00030021, 0x00000003, 0x00000002,
//        0x00030016, 0x00000006, 0x00000020, 0x00040017, 0x00000007, 0x00000006, 0x00000004, 0x00040017,
//        0x00000008, 0x00000006, 0x00000002, 0x0004001e, 0x00000009, 0x00000007, 0x00000008, 0x00040020,
//        0x0000000a, 0x00000003, 0x00000009, 0x0004003b, 0x0000000a, 0x0000000b, 0x00000003, 0x00040015,
//        0x0000000c, 0x00000020, 0x00000001, 0x0004002b, 0x0000000c, 0x0000000d, 0x00000000, 0x00040020,
//        0x0000000e, 0x00000001, 0x00000007, 0x0004003b, 0x0000000e, 0x0000000f, 0x00000001, 0x00040020,
//        0x00000011, 0x00000003, 0x00000007, 0x0004002b, 0x0000000c, 0x00000013, 0x00000001, 0x00040020,
//        0x00000014, 0x00000001, 0x00000008, 0x0004003b, 0x00000014, 0x00000015, 0x00000001, 0x00040020,
//        0x00000017, 0x00000003, 0x00000008, 0x0003001e, 0x00000019, 0x00000007, 0x00040020, 0x0000001a,
//        0x00000003, 0x00000019, 0x0004003b, 0x0000001a, 0x0000001b, 0x00000003, 0x0004003b, 0x00000014,
//        0x0000001c, 0x00000001, 0x0004001e, 0x0000001e, 0x00000008, 0x00000008, 0x00040020, 0x0000001f,
//        0x00000009, 0x0000001e, 0x0004003b, 0x0000001f, 0x00000020, 0x00000009, 0x00040020, 0x00000021,
//        0x00000009, 0x00000008, 0x0004002b, 0x00000006, 0x00000028, 0x00000000, 0x0004002b, 0x00000006,
//        0x00000029, 0x3f800000, 0x00050036, 0x00000002, 0x00000004, 0x00000000, 0x00000003, 0x000200f8,
//        0x00000005, 0x0004003d, 0x00000007, 0x00000010, 0x0000000f, 0x00050041, 0x00000011, 0x00000012,
//        0x0000000b, 0x0000000d, 0x0003003e, 0x00000012, 0x00000010, 0x0004003d, 0x00000008, 0x00000016,
//        0x00000015, 0x00050041, 0x00000017, 0x00000018, 0x0000000b, 0x00000013, 0x0003003e, 0x00000018,
//        0x00000016, 0x0004003d, 0x00000008, 0x0000001d, 0x0000001c, 0x00050041, 0x00000021, 0x00000022,
//        0x00000020, 0x0000000d, 0x0004003d, 0x00000008, 0x00000023, 0x00000022, 0x00050085, 0x00000008,
//        0x00000024, 0x0000001d, 0x00000023, 0x00050041, 0x00000021, 0x00000025, 0x00000020, 0x00000013,
//        0x0004003d, 0x00000008, 0x00000026, 0x00000025, 0x00050081, 0x00000008, 0x00000027, 0x00000024,
//        0x00000026, 0x00050051, 0x00000006, 0x0000002a, 0x00000027, 0x00000000, 0x00050051, 0x00000006,
//        0x0000002b, 0x00000027, 0x00000001, 0x00070050, 0x00000007, 0x0000002c, 0x0000002a, 0x0000002b,
//        0x00000028, 0x00000029, 0x00050041, 0x00000011, 0x0000002d, 0x0000001b, 0x0000000d, 0x0003003e,
//        0x0000002d, 0x0000002c, 0x000100fd, 0x00010038)
//
//// glsl_shader.frag, compiled with:
//// # glslangValidator -V -x -o glsl_shader.frag.u32 glsl_shader.frag
///*
//#version 450 core
//layout(location = 0) out vec4 fColor;
//layout(set=0, binding=0) uniform sampler2D sTexture;
//layout(location = 0) in struct { vec4 Color; vec2 UV; } In;
//void main()
//{
//    fColor = In.Color * texture(sTexture, In.UV.st);
//}
//*/
//val __glsl_shader_frag_spv = intArrayOf(
//        0x07230203, 0x00010000, 0x00080001, 0x0000001e, 0x00000000, 0x00020011, 0x00000001, 0x0006000b,
//        0x00000001, 0x4c534c47, 0x6474732e, 0x3035342e, 0x00000000, 0x0003000e, 0x00000000, 0x00000001,
//        0x0007000f, 0x00000004, 0x00000004, 0x6e69616d, 0x00000000, 0x00000009, 0x0000000d, 0x00030010,
//        0x00000004, 0x00000007, 0x00030003, 0x00000002, 0x000001c2, 0x00040005, 0x00000004, 0x6e69616d,
//        0x00000000, 0x00040005, 0x00000009, 0x6c6f4366, 0x0000726f, 0x00030005, 0x0000000b, 0x00000000,
//        0x00050006, 0x0000000b, 0x00000000, 0x6f6c6f43, 0x00000072, 0x00040006, 0x0000000b, 0x00000001,
//        0x00005655, 0x00030005, 0x0000000d, 0x00006e49, 0x00050005, 0x00000016, 0x78655473, 0x65727574,
//        0x00000000, 0x00040047, 0x00000009, 0x0000001e, 0x00000000, 0x00040047, 0x0000000d, 0x0000001e,
//        0x00000000, 0x00040047, 0x00000016, 0x00000022, 0x00000000, 0x00040047, 0x00000016, 0x00000021,
//        0x00000000, 0x00020013, 0x00000002, 0x00030021, 0x00000003, 0x00000002, 0x00030016, 0x00000006,
//        0x00000020, 0x00040017, 0x00000007, 0x00000006, 0x00000004, 0x00040020, 0x00000008, 0x00000003,
//        0x00000007, 0x0004003b, 0x00000008, 0x00000009, 0x00000003, 0x00040017, 0x0000000a, 0x00000006,
//        0x00000002, 0x0004001e, 0x0000000b, 0x00000007, 0x0000000a, 0x00040020, 0x0000000c, 0x00000001,
//        0x0000000b, 0x0004003b, 0x0000000c, 0x0000000d, 0x00000001, 0x00040015, 0x0000000e, 0x00000020,
//        0x00000001, 0x0004002b, 0x0000000e, 0x0000000f, 0x00000000, 0x00040020, 0x00000010, 0x00000001,
//        0x00000007, 0x00090019, 0x00000013, 0x00000006, 0x00000001, 0x00000000, 0x00000000, 0x00000000,
//        0x00000001, 0x00000000, 0x0003001b, 0x00000014, 0x00000013, 0x00040020, 0x00000015, 0x00000000,
//        0x00000014, 0x0004003b, 0x00000015, 0x00000016, 0x00000000, 0x0004002b, 0x0000000e, 0x00000018,
//        0x00000001, 0x00040020, 0x00000019, 0x00000001, 0x0000000a, 0x00050036, 0x00000002, 0x00000004,
//        0x00000000, 0x00000003, 0x000200f8, 0x00000005, 0x00050041, 0x00000010, 0x00000011, 0x0000000d,
//        0x0000000f, 0x0004003d, 0x00000007, 0x00000012, 0x00000011, 0x0004003d, 0x00000014, 0x00000017,
//        0x00000016, 0x00050041, 0x00000019, 0x0000001a, 0x0000000d, 0x00000018, 0x0004003d, 0x0000000a,
//        0x0000001b, 0x0000001a, 0x00050057, 0x00000007, 0x0000001c, 0x00000017, 0x0000001b, 0x00050085,
//        0x00000007, 0x0000001d, 0x00000012, 0x0000001c, 0x0003003e, 0x00000009, 0x0000001d, 0x000100fd,
//        0x00010038)