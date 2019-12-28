import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.nanovg.NanoVG
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.NULL
import java.awt.Color
import java.awt.Font
import java.awt.Font.MONOSPACED
import java.awt.Font.PLAIN
import java.awt.image.BufferedImage
import java.text.DecimalFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random


private var window: Long = -1


const val GRAVITY = 1
const val GRAVITY_CURVE_OFFSET = 0.3
const val AIR_FRICTION_MULTIPLIER = 0.001
const val TRIAL_MAX_SIZE = 100
const val DISTANCE_MULTIPLIER = 100

const val NANOS_PER_MILLI = 1000000
const val MILLIS_PER_SECOND = 1000


fun main() {
    println("Hello LWJGL " + Version.getVersion() + "!")
    init()
    mainLoop()
    glfwFreeCallbacks(window)
    glfwDestroyWindow(window)
    glfwTerminate()
    glfwSetErrorCallback(null)?.free() ?: println("huh");
}


class Square(var x: Double,
             var y: Double,
             var xVel: Double = 0.0,
             var yVel: Double = 0.0,
             val mass: Double = 10.0,
             val size: Double = mass / 10,
             val color: Color = Color.WHITE,
             val fixed : Boolean = false)

val squares = listOf(
    Square(x = 0.0, y = 0.0, size = 0.01, fixed = true,  mass = 10.0, color = Color.GRAY),
    Square(x = -1.0, y = -1.0, size = 0.01, mass = 1.0, color = Color.RED.darker()),
    Square(x =  0.3, y =  0.7, size = 0.01, mass = 1.0, color = Color.GREEN.darker()),
    Square(x =  0.7, y =  0.5, size = 0.01, mass = 1.0, color = Color.YELLOW.darker()),
    Square(x =  0.5, y =  0.3, size = 0.01, mass = 1.0, color = Color.BLUE.brighter())


    )



val lines = squares.map { it to LinkedList<Pair<Double,Double>>() }.toMap()

fun render() {

    glBegin(GL_LINES)

    for(i in 1 until lines.values.first().size) {
        for(square in squares){
            val (x1,y1) = lines.getValue(square)[i]
            val (x2,y2) = lines.getValue(square)[i - 1]
            glColor4f(square.color.red.toFloat()/255,square.color.green.toFloat()/255, square.color.blue.toFloat()/255, 0.5f)
            glVertex2d(x1,y1)
            glVertex2d(x2,y2)
        }
    }
    glEnd()
    glBegin(GL_QUADS)
    for(square in squares) {
        glColor3f(square.color.red.toFloat()/255,square.color.green.toFloat()/255, square.color.blue.toFloat()/255)
        val s = square.size / 2
        val x = square.x
        val y = square.y
        glVertex2d(x - s, y - s)
        glVertex2d(x + s, y - s)
        glVertex2d(x + s, y + s)
        glVertex2d( x - s, y + s)
    }
    glEnd()
}

fun step() {
    for(base in squares){
        // apply velocity to every square
        if(base.fixed) continue
        for(affecting in squares) {
            if(affecting.mass == 0.0) continue
            if(base === affecting) continue
            val xDiff = base.x - affecting.x
            val yDiff = base.y - affecting.y
            val distance = DISTANCE_MULTIPLIER * sqrt(xDiff.pow(2) + yDiff.pow(2))
            val combinedMass = base.mass + affecting.mass
            val percent = 1 - base.mass / combinedMass
            val totalForce = (combinedMass / (distance + GRAVITY_CURVE_OFFSET).pow(1.5))
//            if((base.x - affecting.x).absoluteValue < 0.001 && (base.y - affecting.y).absoluteValue < 0.001) {
//                // if it's on top of each other
//                error("this happens")
//            }
            val xProp = xDiff / distance
            val yProp = yDiff / distance
            base.xVel += -xProp * percent * totalForce * GRAVITY
            base.yVel += -yProp * percent * totalForce * GRAVITY
        }
    }
    for(b in squares){
        b.x += b.xVel
        b.y += b.yVel
        if(b.x > 1) {
            b.x = 1.0
            b.xVel = 0.0
        }
        if(b.x < -1) {
            b.x = -1.0
            b.xVel = 0.0
        }
        if(b.y > 1) {
            b.y = 1.0
            b.yVel = 0.0
        }
        if(b.y < -1) {
            b.y = -1.0
            b.yVel = 0.0
        }
    }
    for(b in squares) {
        b.xVel -= b.xVel * AIR_FRICTION_MULTIPLIER
        b.yVel -= b.yVel * AIR_FRICTION_MULTIPLIER
        val list = lines.getValue(b)
        list.addLast(b.x to b.y)
        if(list.size > TRIAL_MAX_SIZE)
            list.removeFirst()
    }
}


val firstTime = System.nanoTime()

fun mainLoop() {
    GL.createCapabilities()

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    glEnable(GL_BLEND)
    glClearColor(0f,  0f, 0f, 0f)

    var frames = 0
    while(!glfwWindowShouldClose(window)) {
        frames++
        val seconds = (System.nanoTime() - firstTime).toDouble() / NANOS_PER_MILLI / MILLIS_PER_SECOND
//        val startTime = System.nanoTime()
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        // here is where we do the parsing
        render()
        step()
        glfwSwapBuffers(window)
        glfwPollEvents()
//        val endTime = System.nanoTime()
//        val duration = endTime - startTime
//        if(duration > NANOS_PER_LOOP) {
//            print("loop took too long: ${duration / NANOS_PER_MILLI}ms ")
//        } else {
//            Thread.sleep((NANOS_PER_LOOP - duration) / NANOS_PER_MILLI)
//        }
        println("frame ${frames++.toString().padEnd(5)} seconds: ${seconds.niceStr} fps: " + (frames / seconds).niceStr)
    }
}


private val Double.niceStr: String
    get(){
        return DecimalFormat("####.##").format(this).padEnd(5)
    }


fun init() {
    GLFWErrorCallback.createPrint(System.err).set()

    if (!glfwInit()) {
        error("Can't init GLFW")
    }
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

    window = glfwCreateWindow(700, 700, "FLOAT", NULL, NULL)

    if (window == NULL) error("couldn't create window :(")

    glfwSetKeyCallback(window) { window, key, _, action, _ ->
        if (action == GLFW_RELEASE) {
            println("key pressed: $key")
            if (key == GLFW_KEY_ESCAPE) {
                glfwSetWindowShouldClose(window, true) // detected in rendering loop
            }
        }
    }
    stackPush().use { stack ->
        val width = stack.mallocInt(1)
        val height = stack.mallocInt(1)
        glfwGetWindowSize(window, width, height)
        glfwGetVideoMode(glfwGetPrimaryMonitor())?.let { vidmode ->
            glfwSetWindowPos(window, (vidmode.width() - width.get(0)) / 2,
                (vidmode.height() - height.get(0)) / 2)
        }
    }

    glfwMakeContextCurrent(window)
    glfwSwapInterval(1)
    glfwShowWindow(window)

}




