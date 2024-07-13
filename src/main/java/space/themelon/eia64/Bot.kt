package space.themelon.eia64

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import space.themelon.eia64.analysis.Parser
import java.io.File
import space.themelon.eia64.runtime.Executor
import space.themelon.eia64.syntax.Lexer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.util.StringJoiner
import kotlin.concurrent.thread

object Bot {

    @JvmStatic
    fun main(args: Array<String>) {
        val userDirectory = System.getProperty("user.dir")
        Executor.STD_LIB = File(userDirectory, "eialib/stdlib/").absolutePath
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

                command("lex") {
                    message.text?.let {
                        handleLexRequest(it)
                    }
                }

                command("parse") {
                    message.text?.let {
                        handleParseRequest(it)
                    }
                }
            }
        }
        bot.startPolling()
    }

    private fun CommandHandlerEnvironment.handleParseRequest(request: String) {
        val code = request.substring("/parse ".length)
        try {
            val parsed = Parser(Executor()).parse(Lexer(code).tokens)
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "```\n$parsed\n```",
                parseMode = ParseMode.MARKDOWN_V2,
            )
        } catch (e: IOException) {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = e.message.toString(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }
    }

    private fun CommandHandlerEnvironment.handleLexRequest(request: String) {
        val code = request.substring("/lex ".length)
        try {
            val joiner = StringJoiner("\n")
            Lexer(code).tokens.forEach { joiner.add(it.toString()) }
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "```\n$joiner\n```",
                parseMode = ParseMode.MARKDOWN_V2,
            )
        } catch (e: Exception) {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = e.message.toString(),
                parseMode = ParseMode.MARKDOWN_V2
            )
        }
    }

    private fun CommandHandlerEnvironment.handleRequest(it: String) {
        val code = it.substring("/eia ".length)

        try {
            tryExecution(code)
        } catch (e: Exception) {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = e.message.toString(),
                parseMode = ParseMode.MARKDOWN
            )
        }

    }

    private fun CommandHandlerEnvironment.tryExecution(code: String) {
        val executor = Executor()
        val codeOutput = ByteArrayOutputStream()
        executor.STANDARD_OUTPUT = PrintStream(codeOutput)

        executor.loadMainSource(code)
        bot.sendMessage(
            replyToMessageId = message.messageId,
            chatId = ChatId.fromId(message.chat.id),
            text = codeOutput.toString()
            // this may disrupt formatting: parseMode = ParseMode.MARKDOWN
        )
    }
}