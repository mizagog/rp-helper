import kotlinext.js.js
import kotlinx.html.*
import kotlinx.html.js.*
import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.*
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Math

interface Eh {
    fun updateAndSave()
    fun remove(d: CounterDate)
    fun duplicate(d: CounterDate)
    fun linkOK(counterID: String, increment: Double, mod: Int): Boolean
    fun getCounter(linkedCounterID: String): CounterDate?
    fun setStartElement(start: CounterDate)
    fun locationChange(e: Event, data: CounterDate)
    fun linkCounter(e: Event, data: CounterDate)
    //    fun moveUp(d: CounterDate)
//    fun moveDown(d: CounterDate)
//    fun swap(me: CounterDate, otherHash: Int)
}

var oldVal: String = ""
fun RBuilder.counterUI(data: CounterDate, eh: Eh) {
    div(classes = "counter_root") {
        attrs.jsStyle { if (data.isEditMode) border = "solid thin" }
        div {
            attrs.jsStyle { display = "flex" }
            button(classes = "edit_button") {
                +"E"
                attrs {
                    id = data.id
                    onClickFunction = {
                        data.isEditMode = !data.isEditMode
                        eh.updateAndSave()
                    }
                    onTouchEndFunction = {
                        eh.locationChange(it, data)
                    }
                    onTouchStartFunction = {
                        eh.setStartElement(data)
                    }
//                    element.addEventListener("touchstart", { startElement = getDataByPosition(it)?.first })
//                    element.addEventListener("touchmove", { if (startElement != null) it.preventDefault() })
//                    element.addEventListener("touchend", callback)
                }
            }
            div(if (data.isHeadline) "headline" else "") {
                inputBind(InputType.text, true, "", data.freeText, eh) {
                    data.freeText = it
                }
                attrs.onKeyUpFunction = {

                    if (it.asDynamic().key == "Enter")
                        console.log("wow")
                }
            }
            if (data.isShowCount) {
                span {
                    +": "
                    attrs.jsStyle { fontSize = "larger" }
                }

                inputBind(InputType.number, true, "", data.currentCount.toString(), eh)
                {
                    data.currentCount = it.toDouble()
                    checkMinMax(data)
                }
            }
            span {
                +if (data.max != 0.0) "/ ${data.max}" else ""
                attrs.jsStyle { marginTop = "3px" }
            }
            if (data.isShowCount && !data.isHideActionButtons) {
                div {
                    attrs.jsStyle { marginLeft = "Auto" }
                    button(classes = "action_buttons") {
                        +"+"
                        attrs.onClickFunction = {
                            increment(1, data, eh)
                        }
                    }
                    button(classes = "action_buttons") {
                        +"-"
                        attrs.onClickFunction = {
                            increment(-1, data, eh)
                        }
                    }
                }
            }
            if (data.showUseButton) {
                button {
                    +"${data.linkIncrement} ${eh.getCounter(data.linkedCounterID)?.freeText}"
                    attrs.onClickFunction = {
                        incrementLinked(-1, data, eh)
                    }
                }
            }
        }
        if (data.isEditMode) {
            button {
                +"Del"
                attrs.jsStyle {
                    width = "40px"
                    marginLeft = "auto"
                }
                attrs {
                    onClickFunction = {
                        eh.remove(data)
                    }
                }
            }
            div {
                attrs.style = js {
                    paddingLeft = "60px"
                }
                button {
                    +"Duplicate"
                    attrs {
                        onClickFunction = {
                            data.isEditMode = false
                            eh.duplicate(data)
                        }
                    }
                }
                br { }
                button {
                    +"Link"
                    attrs.id = data.id
                    attrs {
                        onDoubleClickFunction = {
                            data.linkedCounterID = ""
                            data.showUseButton = false
                            eh.updateAndSave()
                        }
                        onTouchStartFunction = {
                            eh.setStartElement(data)
                        }

                        onTouchEndFunction = {
                            eh.linkCounter(it, data)
                        }
                    }
                }
                if (data.linkedCounterID.isNotEmpty()) {
                    inputBind(InputType.number, false, "Link increment:", data.linkIncrement.toString(), eh) {
                        data.linkIncrement = it.toDouble()
                    }
                    inputCheckboxBind(eh, "Show Use Button?", data.showUseButton, {
                        data.showUseButton = it
                        data.isHideActionButtons = if (it == true) true else data.isHideActionButtons
                    })
                }
                inputCheckboxBind(eh, "Show counter?", data.isShowCount, {
                    data.isShowCount = it
                })
                if (data.isShowCount) {
                    inputCheckboxBind(eh, "Hide action buttons?", data.isHideActionButtons, {
                        data.isHideActionButtons = it
                    })
                    inputBind(InputType.number, false, "Change increment:", data.increment.toString(), eh) {
                        data.increment = it.toDouble()
                    }
                    inputBind(InputType.number, false, "Change Max:", data.max.toString(), eh) {
                        data.max = it.toDouble()
                        data.currentCount = data.max
                        checkMinMax(data)
                    }
                    inputBind(InputType.number, false, "Change Min:", data.min.toString(), eh) {
                        data.min = it.toDouble()
                        data.currentCount = data.min
                        checkMinMax(data)
                    }
                }
            }
        }
    }
}

fun incrementLinked(mod: Int, data: CounterDate, eh: Eh) {
    if (eh.linkOK(data.linkedCounterID, data.linkIncrement, mod))
        increment(mod, data, eh)
}

private fun RBuilder.inputCheckboxBind(eh: Eh, label: String, b: Boolean, f: (Boolean) -> Unit) {
    div {
        input(InputType.checkBox) {
            attrs {
                checked = b
                onChangeFunction = {
                    f(it.currentTarget.asDynamic().checked as Boolean)
                    eh.updateAndSave()
                }
            }
        }
        label {
            +label
        }
    }
}

private fun increment(mod: Int, data: CounterDate, eh: Eh) {
    val newVal = data.currentCount + data.increment * mod
    if (newVal >= data.min && (newVal <= data.max || data.max == 0.0)) {
//        console.log(data)
        data.currentCount = newVal
        checkMinMax(data)
        eh.updateAndSave()
    }
//    console.log(newVal)
}

private fun checkMinMax(data: CounterDate) {
    if (data.max != 0.0)
        data.currentCount = minOf(data.currentCount, data.max)
    data.currentCount = maxOf(data.currentCount, data.min)
}

private fun RBuilder.inputBind(inputType: InputType, isDoubleClick: Boolean, desc: String, label: String, eh: Eh, f: (v: String) -> Unit) {
    div {
        label { +desc }
        val classes = if (isDoubleClick) "counter_name" else ""
//        input(type = inputType, classes = classes) {
        textArea( classes = classes) {
            if (isDoubleClick) {
                val baseHeight = 18.0
                val w = when {
                    label.isEmpty() -> 100.0
                    else -> (label.length + 2) * 8.0
                }
                val h = when {
                    label.isEmpty() -> baseHeight
                    else -> label.split(Regex("\n")).size * baseHeight
                }
                attrs.jsStyle {
                    width = "${w}px"
                    height = "${h}px"
                }
            }
            attrs {
                //                wrap = js { "hard" }
//                if (isDoubleClick) {
                    readonly = true
                    val dcFun: (Event) -> Unit = {
                        val asDynamic = it.currentTarget.asDynamic()
//                        if (document.activeElement == it.currentTarget && it.currentTarget.asDynamic().readOnly == true)
                        if (asDynamic.readOnly == true)
                            asDynamic.select()
                        asDynamic.readOnly = false
                    }
                    onDoubleClickFunction = dcFun
                    onMouseUpFunction = dcFun
                    // Fix for Safari mobile keyboard - need to handle touch separately
                    onTouchStartFunction = { e ->
                        val asDynamic = e.currentTarget.asDynamic()
                        // Remove readonly BEFORE focus happens so Safari will show keyboard
                        if (asDynamic.readOnly == true) {
                            asDynamic.readOnly = false
                        }
                    }
                    onTouchEndFunction = { e ->
                        val asDynamic = e.currentTarget.asDynamic()
                        asDynamic.focus()
                        asDynamic.select()
                    }
                    onBlurFunction = {
                        it.currentTarget.asDynamic().readOnly = true
                        document.asDynamic().getSelection().empty()
                        window.asDynamic().getSelection().removeAllRanges()
                        it.currentTarget.asDynamic().blur()
                    }
//                } else {
//                    onFocusFunction = {
//                        it.currentTarget.asDynamic().select()
//                    }
//                }
                value = label
                onKeyDownFunction = {
                    oldVal = it.currentTarget.asDynamic().value as String
                }
                onChangeFunction = {
                    it.currentTarget.asDynamic().style.width = (it.currentTarget.asDynamic().value.length + 1) * 7
                    var v: String = it.currentTarget.asDynamic().value

                    if (inputType == InputType.number) {
//                        if (oldVal == "0") v = v.replace("0", "")
                        console.log(v)
//                        if (v.toDoubleOrNull() == null) v = "0"
                        v = v.toDoubleOrNull()?.toString() ?: "0"
                    }
                    f(v)
                    eh.updateAndSave()
                }
            }
        }
    }
}

data class CounterDate(
        var freeText: String = "Counter",
        var currentCount: Double = 0.0,
        var increment: Double = 1.0,
        var max: Double = 0.0,
        var min: Double = 0.0,
        var isEditMode: Boolean = false,
        var isShowCount: Boolean = true,
        var isHideActionButtons: Boolean = false,
        var isHeadline: Boolean = true,
        var linkedCounterID: String = "",
        var linkIncrement: Double = 1.0,
        var showUseButton: Boolean = false
) {
    var id = makeId()
}

fun makeId(): String {
    var text = ""
    val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    var cout = 0
    possible.forEach {
        if (cout < 10) {
            text += possible[(Math.floor(Math.random() * possible.length))]
            cout++
        }
    }

    return text
}

fun Int.toString(radix: Int): String {
    val value = this
    @Suppress("UnsafeCastFromDynamic")
    return js(code = "value.toString(radix)")
}
