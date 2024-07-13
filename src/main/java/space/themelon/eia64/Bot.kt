package space.themelon.eia64

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import java.io.File
import space.themelon.eia64.runtime.Executor
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.concurrent.thread

object Bot {
    @JvmStatic
    fun main(args: Array<String>) {
        val userDirectory = System.getProperty("user.dir")
        // read the telegram bot api key from KEY file
        val apiToken = File(userDirectory, "KEY").readText()
        val bot = bot {
            token = apiToken

            dispatch {
                command("eia") {
                    message.text?.let {
                        thread {
                            handleRequest(it)
                        }
                    }
                }

                command("help") {
                    bot.sendMessage(
                        text = """
                        Hi, I'm Eia64 Bot!
                        Github: github.com/XomaDev/Eia64
                        To run Eia64 please use format `/eia <code>`
                    """.trimIndent(),
                        chatId = ChatId.fromId(message.chat.id),
                    )
                }
            }
        }
        bot.startPolling()
    }

    private fun CommandHandlerEnvironment.handleRequest(it: String) {
        val code = it.substring("/eia ".length)

        try {
            val executor = Executor()
            val codeOutput = ByteArrayOutputStream()
            executor.STANDARD_OUTPUT = PrintStream(codeOutput)

            val startTime = System.currentTimeMillis()
            executor.loadMainSource(code)
            val endTime = System.currentTimeMillis()
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "`${endTime - startTime} ms`\n$codeOutput",
                // this may disrupt formatting: parseMode = ParseMode.MARKDOWN
            )
        } catch (e: Exception) {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = e.message.toString(),
                parseMode = ParseMode.MARKDOWN
            )
        }

    }
}