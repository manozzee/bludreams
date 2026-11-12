package com.lagradost.cloudstream3.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Centralized Phosphor Icons mapping system for Compose UI across CloudStream.
 * Uses authentic 256x256 Phosphor Icons (Regular, Bold, and Fill weights).
 */
object AppIcons {
    val Default: AppIcons get() = this
    val Filled: AppIcons get() = this
    val Outlined: AppIcons get() = this
    val Rounded: AppIcons get() = this
    val AutoMirrored: AppIcons get() = this

    private fun phosphorIcon(
        name: String,
        pathData: String,
        autoMirror: Boolean = false,
        fillType: PathFillType = PathFillType.NonZero,
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f,
            autoMirror = autoMirror,
        ).addPath(
            pathData = addPathNodes(pathData),
            pathFillType = fillType,
            fill = SolidColor(Color.White),
            stroke = null,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
        ).build()
    }

    // Phosphor: Play (Fill)
    val PlayArrow: ImageVector by lazy {
        phosphorIcon(
            name = "PlayArrow",
            pathData = "M240,128a15.74,15.74,0,0,1-7.6,13.51L88.32,229.65a16,16,0,0,1-16.2.3A15.86,15.86,0,0,1,64,216.13V39.87a15.86,15.86,0,0,1,8.12-13.82,16,16,0,0,1,16.2.3L232.4,114.49A15.74,15.74,0,0,1,240,128Z",
        )
    }

    // Phosphor: Info (Regular)
    val Info: ImageVector by lazy {
        phosphorIcon(
            name = "Info",
            pathData = "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm0,192a88,88,0,1,1,88-88A88.1,88.1,0,0,1,128,216Zm16-40a8,8,0,0,1-8,8,16,16,0,0,1-16-16V128a8,8,0,0,1,0-16,16,16,0,0,1,16,16v40A8,8,0,0,1,144,176ZM112,84a12,12,0,1,1,12,12A12,12,0,0,1,112,84Z",
        )
    }

    // Phosphor: BookmarkSimple (Fill)
    val Bookmark: ImageVector by lazy {
        phosphorIcon(
            name = "Bookmark",
            pathData = "M184,32H72A16,16,0,0,0,56,48V224a8,8,0,0,0,12.24,6.78L128,193.43l59.77,37.35A8,8,0,0,0,200,224V48A16,16,0,0,0,184,32Z",
        )
    }

    // Phosphor: BookmarkSimple (Regular)
    val BookmarkBorder: ImageVector by lazy {
        phosphorIcon(
            name = "BookmarkBorder",
            pathData = "M184,32H72A16,16,0,0,0,56,48V224a8,8,0,0,0,12.24,6.78L128,193.43l59.77,37.35A8,8,0,0,0,200,224V48A16,16,0,0,0,184,32Zm0,177.57-51.77-32.35a8,8,0,0,0-8.48,0L72,209.57V48H184Z",
        )
    }

    // Phosphor: Star (Fill)
    val Star: ImageVector by lazy {
        phosphorIcon(
            name = "Star",
            pathData = "M234.29,114.85l-45,38.83L203,211.75a16.4,16.4,0,0,1-24.5,17.82L128,198.49,77.47,229.57A16.4,16.4,0,0,1,53,211.75l13.76-58.07-45-38.83A16.46,16.46,0,0,1,31.08,86l59-4.76,22.76-55.08a16.36,16.36,0,0,1,30.27,0l22.75,55.08,59,4.76a16.46,16.46,0,0,1,9.37,28.86Z",
        )
    }

    // Phosphor: Check (Bold)
    val Check: ImageVector by lazy {
        phosphorIcon(
            name = "Check",
            pathData = "M232.49,80.49l-128,128a12,12,0,0,1-17,0l-56-56a12,12,0,1,1,17-17L96,183,215.51,63.51a12,12,0,0,1,17,17Z",
        )
    }

    // Phosphor: CaretRight (Bold)
    val KeyboardArrowRight: ImageVector by lazy {
        phosphorIcon(
            name = "KeyboardArrowRight",
            autoMirror = true,
            pathData = "M184.49,136.49l-80,80a12,12,0,0,1-17-17L159,128,87.51,56.49a12,12,0,1,1,17-17l80,80A12,12,0,0,1,184.49,136.49Z",
        )
    }

    // Phosphor: ArrowLeft (Regular)
    val ArrowBack: ImageVector by lazy {
        phosphorIcon(
            name = "ArrowBack",
            autoMirror = true,
            pathData = "M224,128a8,8,0,0,1-8,8H59.31l58.35,58.34a8,8,0,0,1-11.32,11.32l-72-72a8,8,0,0,1,0-11.32l72-72a8,8,0,0,1,11.32,11.32L59.31,120H216A8,8,0,0,1,224,128Z",
        )
    }

    // Phosphor: DotsThreeVertical (Bold)
    val MoreVert: ImageVector by lazy {
        phosphorIcon(
            name = "MoreVert",
            pathData = "M144,128a16,16,0,1,1-16-16A16,16,0,0,1,144,128ZM128,72a16,16,0,1,0-16-16A16,16,0,0,0,128,72Zm0,112a16,16,0,1,0,16,16A16,16,0,0,0,128,184Z",
        )
    }

    // Phosphor: CaretDown (Bold)
    val ExpandMore: ImageVector by lazy {
        phosphorIcon(
            name = "ExpandMore",
            pathData = "M216.49,104.49l-80,80a12,12,0,0,1-17,0l-80-80a12,12,0,0,1,17-17L128,159l71.51-71.52a12,12,0,0,1,17,17Z",
        )
    }

    // Phosphor: CaretUp (Bold)
    val ChevronUp: ImageVector by lazy {
        phosphorIcon(
            name = "ChevronUp",
            pathData = "M216.49,168.49a12,12,0,0,1-17,0L128,97,56.49,168.49a12,12,0,0,1-17-17l80-80a12,12,0,0,1,17,0l80,80A12,12,0,0,1,216.49,168.49Z",
        )
    }

    // Phosphor: ArrowClockwise (Regular)
    val Refresh: ImageVector by lazy {
        phosphorIcon(
            name = "Refresh",
            pathData = "M240,56v48a8,8,0,0,1-8,8H184a8,8,0,0,1,0-16H211.4L184.81,71.64l-.25-.24a80,80,0,1,0-1.67,114.78,8,8,0,0,1,11,11.63A95.44,95.44,0,0,1,128,224h-1.32A96,96,0,1,1,195.75,60L224,85.8V56a8,8,0,1,1,16,0Z",
        )
    }

    // Phosphor: MagnifyingGlass (Regular)
    val Search: ImageVector by lazy {
        phosphorIcon(
            name = "Search",
            pathData = "M229.66,218.34l-50.07-50.06a88.11,88.11,0,1,0-11.31,11.31l50.06,50.07a8,8,0,0,0,11.32-11.32ZM40,112a72,72,0,1,1,72,72A72.08,72.08,0,0,1,40,112Z",
        )
    }

    // Phosphor: SlidersHorizontal (Regular)
    val Tune: ImageVector by lazy {
        phosphorIcon(
            name = "Tune",
            pathData = "M40,88H73a32,32,0,0,0,62,0h81a8,8,0,0,0,0-16H135a32,32,0,0,0-62,0H40a8,8,0,0,0,0,16Zm64-24A16,16,0,1,1,88,80,16,16,0,0,1,104,64ZM216,168H199a32,32,0,0,0-62,0H40a8,8,0,0,0,0,16h97a32,32,0,0,0,62,0h17a8,8,0,0,0,0-16Zm-48,24a16,16,0,1,1,16-16A16,16,0,0,1,168,192Z",
        )
    }

    // Phosphor: UserCircle (Regular)
    val AccountCircle: ImageVector by lazy {
        phosphorIcon(
            name = "AccountCircle",
            pathData = "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24ZM74.08,197.5a64,64,0,0,1,107.84,0,87.83,87.83,0,0,1-107.84,0ZM96,120a32,32,0,1,1,32,32A32,32,0,0,1,96,120Zm97.76,66.41a79.66,79.66,0,0,0-36.06-28.75,48,48,0,1,0-59.4,0,79.66,79.66,0,0,0-36.06,28.75,88,88,0,1,1,131.52,0Z",
        )
    }

    // Phosphor: SpeakerSlash (Regular)
    val VolumeOff: ImageVector by lazy {
        phosphorIcon(
            name = "VolumeOff",
            autoMirror = true,
            pathData = "M53.92,34.62A8,8,0,1,0,42.08,45.38L73.55,80H32A16,16,0,0,0,16,96v64a16,16,0,0,0,16,16H77.25l69.84,54.31A8,8,0,0,0,160,224V175.09l42.08,46.29a8,8,0,1,0,11.84-10.76ZM32,96H72v64H32ZM144,207.64,88,164.09V95.9l56,61.6Zm54-106.56a40,40,0,0,1,0,53.84,8,8,0,0,1-12-10.58,24,24,0,0,0,0-32.68,8,8,0,0,1,12-10.58ZM248,128a79.9,79.9,0,0,1-20.37,53.34,8,8,0,0,1-11.92-10.67,64,64,0,0,0,0-85.33,8,8,0,1,1,11.92-10.67A79.83,79.83,0,0,1,248,128ZM144,48.36v42.5a8,8,0,1,0,16,0V32a8,8,0,0,0-12.91-6.31l-39.75,30.9a8,8,0,1,0,9.82,12.63Z",
        )
    }

    // Phosphor: SpeakerHigh (Regular)
    val VolumeUp: ImageVector by lazy {
        phosphorIcon(
            name = "VolumeUp",
            autoMirror = true,
            pathData = "M155.51,24.81a8,8,0,0,0-8.42.88L77.25,80H32A16,16,0,0,0,16,96v64a16,16,0,0,0,16,16H77.25l69.84,54.31A8,8,0,0,0,160,224V32A8,8,0,0,0,155.51,24.81ZM32,96H72v64H32ZM144,207.64,88,164.09V91.91l56-43.55Zm54-106.56a40,40,0,0,1,0,53.84,8,8,0,0,1-12-10.58,24,24,0,0,0,0-32.68,8,8,0,0,1,12-10.58ZM248,128a79.9,79.9,0,0,1-20.37,53.34,8,8,0,0,1-11.92-10.67,64,64,0,0,0,0-85.33,8,8,0,1,1,11.92-10.67A79.83,79.83,0,0,1,248,128Z",
        )
    }

    // Phosphor: ArrowsClockwise (Regular)
    val Sync: ImageVector by lazy {
        phosphorIcon(
            name = "Sync",
            pathData = "M224,48V96a8,8,0,0,1-8,8H168a8,8,0,0,1,0-16h28.69L182.06,73.37a79.56,79.56,0,0,0-56.13-23.43h-.45A79.52,79.52,0,0,0,69.59,72.71,8,8,0,0,1,58.41,61.27a96,96,0,0,1,135,.79L208,76.69V48a8,8,0,0,1,16,0ZM186.41,183.29a80,80,0,0,1-112.47-.66L59.31,168H88a8,8,0,0,0,0-16H40a8,8,0,0,0-8,8v48a8,8,0,0,0,16,0V179.31l14.63,14.63A95.43,95.43,0,0,0,130,222.06h.53a95.36,95.36,0,0,0,67.07-27.33,8,8,0,0,0-11.18-11.44Z",
        )
    }

    // Phosphor: ShareNetwork (Regular)
    val Share: ImageVector by lazy {
        phosphorIcon(
            name = "Share",
            pathData = "M176,160a39.89,39.89,0,0,0-28.62,12.09l-46.1-29.63a39.8,39.8,0,0,0,0-28.92l46.1-29.63a40,40,0,1,0-8.66-13.45l-46.1,29.63a40,40,0,1,0,0,55.82l46.1,29.63A40,40,0,1,0,176,160Zm0-128a24,24,0,1,1-24,24A24,24,0,0,1,176,32ZM64,152a24,24,0,1,1,24-24A24,24,0,0,1,64,152Zm112,72a24,24,0,1,1,24-24A24,24,0,0,1,176,224Z",
        )
    }

    // Phosphor: Globe (Regular)
    val Public: ImageVector by lazy {
        phosphorIcon(
            name = "Public",
            pathData = "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24ZM215.63,120H175.81c-1.45-28.46-10.27-55.49-25.52-77.82A88.19,88.19,0,0,1,215.63,120ZM96.23,136h63.54c-1.82,29.27-12,56.69-29.77,77.29C112.2,192.69,98.05,165.27,96.23,136Zm0-16c1.82-29.27,12-56.69,29.77-77.29,17.8,20.6,31.95,48,33.77,77.29ZM105.71,42.18C90.46,64.51,81.64,91.54,80.19,120H40.37A88.19,88.19,0,0,1,105.71,42.18ZM40.37,136H80.19c1.45,28.46,10.27,55.49,25.52,77.82A88.19,88.19,0,0,1,40.37,136Zm109.92,77.82c15.25-22.33,24.07-49.36,25.52-77.82h39.82A88.19,88.19,0,0,1,150.29,213.82Z",
        )
    }

    // Phosphor: Heart (Fill)
    val Favorite: ImageVector by lazy {
        phosphorIcon(
            name = "Favorite",
            pathData = "M240,102c0,70-103.79,126.66-108.21,129a8,8,0,0,1-7.58,0C119.79,228.66,16,172,16,102A62.07,62.07,0,0,1,78,40c20.65,0,38.73,8.88,50,23.89C139.27,48.88,157.35,40,178,40A62.07,62.07,0,0,1,240,102Z",
        )
    }

    // Phosphor: Heart (Regular)
    val FavoriteBorder: ImageVector by lazy {
        phosphorIcon(
            name = "FavoriteBorder",
            pathData = "M178,40c-20.65,0-38.73,8.88-50,23.89C116.73,48.88,98.65,40,78,40a62.07,62.07,0,0,0-62,62c0,70,103.79,126.66,108.21,129a8,8,0,0,0,7.58,0C136.21,228.66,240,172,240,102A62.07,62.07,0,0,0,178,40ZM128,214.8C109.74,204.16,32,155.69,32,102A46.06,46.06,0,0,1,78,56c19.45,0,35.78,10.36,42.6,27a8,8,0,0,0,14.8,0c6.82-16.67,23.15-27,42.6-27a46.06,46.06,0,0,1,46,46C224,155.61,146.24,204.15,128,214.8Z",
        )
    }

    // Phosphor: Bell (Fill)
    val Notifications: ImageVector by lazy {
        phosphorIcon(
            name = "Notifications",
            pathData = "M221.8,175.94C216.25,166.38,208,139.33,208,104a80,80,0,1,0-160,0c0,35.34-8.26,62.38-13.81,71.94A16,16,0,0,0,48,200H88.81a40,40,0,0,0,78.38,0H208a16,16,0,0,0,13.8-24.06ZM128,216a24,24,0,0,1-22.62-16h45.24A24,24,0,0,1,128,216Z",
        )
    }

    // Phosphor: Bell (Regular)
    val NotificationsNone: ImageVector by lazy {
        phosphorIcon(
            name = "NotificationsNone",
            pathData = "M221.8,175.94C216.25,166.38,208,139.33,208,104a80,80,0,1,0-160,0c0,35.34-8.26,62.38-13.81,71.94A16,16,0,0,0,48,200H88.81a40,40,0,0,0,78.38,0H208a16,16,0,0,0,13.8-24.06ZM128,216a24,24,0,0,1-22.62-16h45.24A24,24,0,0,1,128,216ZM48,184c7.7-13.24,16-43.92,16-80a64,64,0,1,1,128,0c0,36.05,8.28,66.73,16,80Z",
        )
    }

    // Phosphor: DownloadSimple (Regular)
    val Download: ImageVector by lazy {
        phosphorIcon(
            name = "Download",
            pathData = "M224,144v64a8,8,0,0,1-8,8H40a8,8,0,0,1-8-8V144a8,8,0,0,1,16,0v56H208V144a8,8,0,0,1,16,0Zm-101.66,5.66a8,8,0,0,0,11.32,0l40-40a8,8,0,0,0-11.32-11.32L136,124.69V32a8,8,0,0,0-16,0v92.69L93.66,98.34a8,8,0,0,0-11.32,11.32Z",
        )
    }

    // Phosphor: FilmStrip (Regular)
    val Movie: ImageVector by lazy {
        phosphorIcon(
            name = "Movie",
            pathData = "M216,40H40A16,16,0,0,0,24,56V200a16,16,0,0,0,16,16H216a16,16,0,0,0,16-16V56A16,16,0,0,0,216,40Zm0,16V72H184V56ZM168,56V72H136V56ZM120,56V72H88V56ZM72,56V72H40V56ZM40,88H216v80H40Zm0,96H72v16H40Zm48,0h32v16H88Zm48,0h32v16H136Zm48,0h32v16H184Z",
        )
    }

    // Phosphor: SortAscending (Regular)
    val Sort: ImageVector by lazy {
        phosphorIcon(
            name = "Sort",
            autoMirror = true,
            pathData = "M128,128a8,8,0,0,1-8,8H48a8,8,0,0,1,0-16h72A8,8,0,0,1,128,128ZM48,72H184a8,8,0,0,0,0-16H48a8,8,0,0,0,0,16Zm56,112H48a8,8,0,0,0,0,16h56a8,8,0,0,0,0-16Zm125.66-21.66a8,8,0,0,0-11.32,0L192,188.69V112a8,8,0,0,0-16,0v76.69l-26.34-26.35a8,8,0,0,0-11.32,11.32l40,40a8,8,0,0,0,11.32,0l40-40A8,8,0,0,0,229.66,162.34Z",
        )
    }

    // Phosphor: FunnelSimple (Regular)
    val FilterList: ImageVector by lazy {
        phosphorIcon(
            name = "FilterList",
            pathData = "M200,136a8,8,0,0,1-8,8H64a8,8,0,0,1,0-16H192A8,8,0,0,1,200,136Zm32-56H24a8,8,0,0,0,0,16H232a8,8,0,0,0,0-16Zm-80,96H104a8,8,0,0,0,0,16h48a8,8,0,0,0,0-16Z",
        )
    }

    // Phosphor: PuzzlePiece (Regular)
    val Extension: ImageVector by lazy {
        phosphorIcon(
            name = "Extension",
            pathData = "M220.27,158.54a8,8,0,0,0-7.7-.46,20,20,0,1,1,0-36.16,8,8,0,0,0,7.7-.46A8,8,0,0,0,224,114.69V72a16,16,0,0,0-16-16H171.78a35.36,35.36,0,0,0,.22-4,36.11,36.11,0,0,0-11.36-26.24,36,36,0,0,0-60.55,23.62,36.56,36.56,0,0,0,.14,6.62H64A16,16,0,0,0,48,72v32.22a35.36,35.36,0,0,0-4-.22,36.12,36.12,0,0,0-26.24,11.36,35.7,35.7,0,0,0-9.69,27,36.08,36.08,0,0,0,33.31,33.6,35.68,35.68,0,0,0,6.62-.14V208a16,16,0,0,0,16,16H208a16,16,0,0,0,16-16V165.31A8,8,0,0,0,220.27,158.54ZM208,208H64V165.31a8,8,0,0,0-11.43-7.23,20,20,0,1,1,0-36.16A8,8,0,0,0,64,114.69V72h46.69a8,8,0,0,0,7.23-11.43,20,20,0,1,1,36.16,0A8,8,0,0,0,161.31,72H208v32.23a35.68,35.68,0,0,0-6.62-.14A36,36,0,0,0,204,176a35.36,35.36,0,0,0,4-.22Z",
        )
    }

    // Phosphor: PlayCircle (Regular)
    val NetworkStream: ImageVector by lazy {
        phosphorIcon(
            name = "NetworkStream",
            pathData = "M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm0,192a88,88,0,1,1,88-88A88.1,88.1,0,0,1,128,216Zm36.44-94.66-48-32A8,8,0,0,0,104,96v64a8,8,0,0,0,12.44,6.66l48-32a8,8,0,0,0,0-13.32ZM120,145.05V111l25.58,17Z",
        )
    }

    // Phosphor: ListBullets (Regular)
    val ViewList: ImageVector by lazy {
        phosphorIcon(
            name = "ViewList",
            autoMirror = true,
            pathData = "M80,64a8,8,0,0,1,8-8H216a8,8,0,0,1,0,16H88A8,8,0,0,1,80,64Zm136,56H88a8,8,0,0,0,0,16H216a8,8,0,0,0,0-16Zm0,64H88a8,8,0,0,0,0,16H216a8,8,0,0,0,0-16ZM44,52A12,12,0,1,0,56,64,12,12,0,0,0,44,52Zm0,64a12,12,0,1,0,12,12A12,12,0,0,0,44,116Zm0,64a12,12,0,1,0,12,12A12,12,0,0,0,44,180Z",
        )
    }

    // Phosphor: Cards (Regular)
    val ViewCarousel: ImageVector by lazy {
        phosphorIcon(
            name = "ViewCarousel",
            pathData = "M184,72H40A16,16,0,0,0,24,88V200a16,16,0,0,0,16,16H184a16,16,0,0,0,16-16V88A16,16,0,0,0,184,72Zm0,128H40V88H184V200ZM232,56V176a8,8,0,0,1-16,0V56H64a8,8,0,0,1,0-16H216A16,16,0,0,1,232,56Z",
        )
    }

    // Phosphor: User (Regular)
    val Person: ImageVector by lazy {
        phosphorIcon(
            name = "Person",
            pathData = "M230.92,212c-15.23-26.33-38.7-45.21-66.09-54.16a72,72,0,1,0-73.66,0C63.78,166.78,40.31,185.66,25.08,212a8,8,0,1,0,13.85,8c18.84-32.56,52.14-52,89.07-52s70.23,19.44,89.07,52a8,8,0,1,0,13.85-8ZM72,96a56,56,0,1,1,56,56A56.06,56.06,0,0,1,72,96Z",
        )
    }

    // Phosphor: X (Bold)
    val Close: ImageVector by lazy {
        phosphorIcon(
            name = "Close",
            pathData = "M208.49,191.51a12,12,0,0,1-17,17L128,145,64.49,208.49a12,12,0,0,1-17-17L111,128,47.51,64.49a12,12,0,0,1,17-17L128,111l63.51-63.52a12,12,0,0,1,17,17L145,128Z",
        )
    }

    // Phosphor: Rectangle (Regular)
    val CropLandscape: ImageVector by lazy {
        phosphorIcon(
            name = "CropLandscape",
            pathData = "M216,40H40A16,16,0,0,0,24,56V200a16,16,0,0,0,16,16H216a16,16,0,0,0,16-16V56A16,16,0,0,0,216,40Zm0,160H40V56H216V200Z",
        )
    }

    // Phosphor: Image (Regular)
    val Image: ImageVector by lazy {
        phosphorIcon(
            name = "Image",
            pathData = "M216,40H40A16,16,0,0,0,24,56V200a16,16,0,0,0,16,16H216a16,16,0,0,0,16-16V56A16,16,0,0,0,216,40Zm0,16V158.75l-26.07-26.06a16,16,0,0,0-22.63,0l-20,20-44-44a16,16,0,0,0-22.62,0L40,149.37V56ZM40,172l52-52,80,80H40Zm176,28H194.63l-36-36,20-20L216,181.38V200ZM144,100a12,12,0,1,1,12,12A12,12,0,0,1,144,100Z",
        )
    }

    // Phosphor: Stack (Regular)
    val Layers: ImageVector by lazy {
        phosphorIcon(
            name = "Layers",
            pathData = "M230.91,172A8,8,0,0,1,228,182.91l-96,56a8,8,0,0,1-8.06,0l-96-56A8,8,0,0,1,36,169.09l92,53.65,92-53.65A8,8,0,0,1,230.91,172ZM220,121.09l-92,53.65L36,121.09A8,8,0,0,0,28,134.91l96,56a8,8,0,0,0,8.06,0l96-56A8,8,0,1,0,220,121.09ZM24,80a8,8,0,0,1,4-6.91l96-56a8,8,0,0,1,8.06,0l96,56a8,8,0,0,1,0,13.82l-96,56a8,8,0,0,1-8.06,0l-96-56A8,8,0,0,1,24,80Zm23.88,0L128,126.74,208.12,80,128,33.26Z",
        )
    }

    // Phosphor: CornersOut (Regular)
    val RoundedCorner: ImageVector by lazy {
        phosphorIcon(
            name = "RoundedCorner",
            pathData = "M216,48V88a8,8,0,0,1-16,0V56H168a8,8,0,0,1,0-16h40A8,8,0,0,1,216,48ZM88,200H56V168a8,8,0,0,0-16,0v40a8,8,0,0,0,8,8H88a8,8,0,0,0,0-16Zm120-40a8,8,0,0,0-8,8v32H168a8,8,0,0,0,0,16h40a8,8,0,0,0,8-8V168A8,8,0,0,0,208,160ZM88,40H48a8,8,0,0,0-8,8V88a8,8,0,0,0,16,0V56H88a8,8,0,0,0,0-16Z",
        )
    }

    // Phosphor: TextT (Regular)
    val Title: ImageVector by lazy {
        phosphorIcon(
            name = "Title",
            pathData = "M208,56V88a8,8,0,0,1-16,0V64H136V192h24a8,8,0,0,1,0,16H96a8,8,0,0,1,0-16h24V64H64V88a8,8,0,0,1-16,0V56a8,8,0,0,1,8-8H200A8,8,0,0,1,208,56Z",
        )
    }

    // Phosphor: Columns (Regular)
    val ViewColumn: ImageVector by lazy {
        phosphorIcon(
            name = "ViewColumn",
            pathData = "M104,40H56A16,16,0,0,0,40,56V200a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V56A16,16,0,0,0,104,40Zm0,160H56V56h48V200Zm96-160H152a16,16,0,0,0-16,16V200a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V56A16,16,0,0,0,200,40Zm0,160H152V56h48V200Z",
        )
    }
}
