package com.terminal.app.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import com.terminal.app.databinding.FragmentTerminalBinding
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!

    private var shellProcess: Process? = null
    private var processOutputStream: DataOutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTerminal()
        setupInput()
    }

    private fun setupTerminal() {
        appendOutput("Terminal App v1.0\n")
        appendOutput("Type 'help' for available commands\n\n")
        showPrompt()
    }

    private fun setupInput() {
        binding.commandInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                executeCommand()
                true
            } else false
        }
    }

    private fun executeCommand() {
        val command = binding.commandInput.text.toString().trim()
        if (command.isEmpty()) {
            showPrompt()
            return
        }

        commandHistory.add(command)
        historyIndex = commandHistory.size

        appendOutput("$command\n")
        binding.commandInput.text?.clear()

        when {
            command == "clear" -> {
                binding.terminalOutput.text = ""
                showPrompt()
            }
            command == "help" -> {
                appendOutput("Available commands:\n")
                appendOutput("  clear     - Clear terminal\n")
                appendOutput("  help      - Show this help\n")
                appendOutput("  exit      - Close terminal\n")
                appendOutput("  history   - Show command history\n")
                appendOutput("  echo      - Print text\n")
                appendOutput("  date      - Show date/time\n")
                appendOutput("  whoami    - Show current user\n")
                appendOutput("  uname     - Show system info\n")
                appendOutput("  ls        - List files\n")
                appendOutput("  cat       - Show file contents\n")
                appendOutput("  mkdir     - Create directory\n")
                appendOutput("  touch     - Create file\n")
                appendOutput("  cp        - Copy files\n")
                appendOutput("  mv        - Move files\n")
                appendOutput("  rm        - Remove files\n")
                appendOutput("  chmod     - Change permissions\n\n")
                showPrompt()
            }
            command == "exit" -> {
                appendOutput("Goodbye!\n")
                shellProcess?.destroy()
                requireActivity().finish()
            }
            command == "history" -> {
                commandHistory.forEachIndexed { index, cmd ->
                    appendOutput("  ${index + 1}  $cmd\n")
                }
                appendOutput("\n")
                showPrompt()
            }
            else -> {
                executeShellCommand(command)
            }
        }

        scrollToBottom()
    }

    private fun executeShellCommand(command: String) {
        scope.launch {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                shellProcess = process

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                val output = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    output.appendLine(line)
                }

                while (errorReader.readLine().also { line = it } != null) {
                    output.appendLine(line)
                }

                val exitCode = process.waitFor()

                withContext(Dispatchers.Main) {
                    if (output.isNotEmpty()) {
                        appendOutput(output.toString())
                    }
                    if (exitCode != 0 && output.isEmpty()) {
                        appendOutput("Command failed with exit code $exitCode\n")
                    }
                    appendOutput("\n")
                    showPrompt()
                    scrollToBottom()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendOutput("Error: ${e.message}\n\n")
                    showPrompt()
                    scrollToBottom()
                }
            }
        }
    }

    private fun appendOutput(text: String) {
        binding.terminalOutput.append(text)
    }

    private fun showPrompt() {
        binding.promptText.text = "$ "
    }

    private fun scrollToBottom() {
        binding.terminalScroll.post {
            binding.terminalScroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        shellProcess?.destroy()
        _binding = null
    }
}
