const express = require('express');
const cors = require('cors');
const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json({ limit: '50mb' }));

// Multer setup for audio file uploads
const upload = multer({ 
  dest: 'uploads/',
  limits: { fileSize: 10 * 1024 * 1024 } // 10MB limit
});

// Ensure uploads directory exists
if (!fs.existsSync('uploads')) {
  fs.mkdirSync('uploads');
}

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    timestamp: new Date().toISOString(),
    services: {
      stt: process.env.SONIOX_API_KEY ? 'configured' : 'not configured',
      llm: process.env.OPENAI_API_KEY ? 'configured' : 'not configured'
    }
  });
});

// Main voice command endpoint
app.post('/voice/command', upload.single('audio'), async (req, res) => {
  try {
    console.log('Received voice command request');
    
    const mode = req.body.mode || 'agent';
    const language = req.body.language || 'bn';
    
    let audioBuffer;
    
    // Handle audio from file upload or base64
    if (req.file) {
      audioBuffer = fs.readFileSync(req.file.path);
    } else if (req.body.audioBase64) {
      audioBuffer = Buffer.from(req.body.audioBase64, 'base64');
    } else {
      return res.status(400).json({ error: 'No audio provided' });
    }

    // Step 1: Speech-to-Text (Bangla)
    console.log('Converting speech to text...');
    const transcriptBn = await transcribeAudio(audioBuffer);
    console.log('Transcript (BN):', transcriptBn);

    // Step 2: Process with LLM (translate + intent classification)
    console.log('Processing with LLM...');
    const llmResult = await processWithLLM(transcriptBn, mode);
    console.log('LLM Result:', llmResult);

    // Clean up uploaded file
    if (req.file) {
      fs.unlinkSync(req.file.path);
    }

    // Build response
    const response = {
      transcriptBn: transcriptBn,
      englishText: llmResult.englishText,
      intent: llmResult.intent,
      contactName: llmResult.contactName || null,
      searchQuery: llmResult.searchQuery || null,
      promptForBuilder: llmResult.promptForBuilder || null,
      raw: llmResult
    };

    res.json(response);

  } catch (error) {
    console.error('Error processing voice command:', error);
    res.status(500).json({ 
      error: 'Failed to process voice command',
      details: error.message 
    });
  }
});

// Soniox Bengali STT
async function transcribeAudio(audioBuffer) {
  const apiKey = process.env.SONIOX_API_KEY;
  
  if (!apiKey) {
    // Fallback: return dummy text for testing
    console.log('No Soniox API key, using dummy transcription');
    return "আমি রিয়াজকে কল করতে চাই";
  }

  try {
    const formData = new FormData();
    formData.append('file', audioBuffer, {
      filename: 'audio.m4a',
      contentType: 'audio/m4a'
    });
    formData.append('language', 'bn');
    formData.append('model', 'default');

    const response = await axios.post('https://api.soniox.com/transcribe', formData, {
      headers: {
        ...formData.getHeaders(),
        'Authorization': `Bearer ${apiKey}`
      },
      timeout: 30000
    });

    return response.data.text || response.data.transcript || '';
  } catch (error) {
    console.error('STT Error:', error.response?.data || error.message);
    throw new Error('Speech-to-text failed');
  }
}

// LLM Processing (OpenAI)
async function processWithLLM(banglaText, mode) {
  const apiKey = process.env.OPENAI_API_KEY;
  
  if (!apiKey) {
    // Fallback: simple mock response for testing
    console.log('No OpenAI API key, using dummy LLM response');
    return {
      englishText: "I want to call Riaz.",
      intent: "call_contact",
      contactName: "Riaz",
      searchQuery: null,
      promptForBuilder: null
    };
  }

  const systemPrompt = `You are a voice assistant parser for Bangla speech. 
Your job is to:
1. Translate the Bangla text to natural English
2. Classify the user's intent
3. Extract relevant parameters

Available intents:
- translate_only: Just translate, no action needed
- call_contact: User wants to call someone (extract contactName)
- open_camera: User wants to open camera
- open_youtube: User wants to search YouTube (extract searchQuery)
- lovable_build: User wants to build something with Lovable.dev (extract promptForBuilder)

Respond ONLY with valid JSON in this exact format:
{
  "englishText": "translated English text",
  "intent": "one_of_the_above",
  "contactName": "name if calling",
  "searchQuery": "search terms if YouTube",
  "promptForBuilder": "prompt if building with Lovable"
}`;

  try {
    const response = await axios.post('https://api.openai.com/v1/chat/completions', {
      model: 'gpt-3.5-turbo',
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: `Mode: ${mode}\nBangla text: "${banglaText}"` }
      ],
      temperature: 0.3,
      max_tokens: 500
    }, {
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json'
      },
      timeout: 30000
    });

    const content = response.data.choices[0].message.content;
    
    // Extract JSON from response
    const jsonMatch = content.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
    
    throw new Error('Invalid LLM response format');
  } catch (error) {
    console.error('LLM Error:', error.response?.data || error.message);
    throw new Error('LLM processing failed');
  }
}

// Test endpoint with dummy data
app.post('/voice/command/test', (req, res) => {
  const { testIntent } = req.body;
  
  const testResponses = {
    call_contact: {
      transcriptBn: "আমি রিয়াজকে কল করতে চাই",
      englishText: "I want to call Riaz.",
      intent: "call_contact",
      contactName: "Riaz",
      searchQuery: null,
      promptForBuilder: null
    },
    open_youtube: {
      transcriptBn: "ইউটিউবে AI ইমেজ জেনারেটর সার্চ করো",
      englishText: "Search for AI image generator on YouTube.",
      intent: "open_youtube",
      contactName: null,
      searchQuery: "AI image generator",
      promptForBuilder: null
    },
    lovable_build: {
      transcriptBn: "আমার AI ইমেজ জেনারেটর ওয়েবসাইট বানাও",
      englishText: "Build me an AI image generator website.",
      intent: "lovable_build",
      contactName: null,
      searchQuery: null,
      promptForBuilder: "Create an AI image generator website with a clean interface"
    },
    open_camera: {
      transcriptBn: "ক্যামেরা খুলো",
      englishText: "Open the camera.",
      intent: "open_camera",
      contactName: null,
      searchQuery: null,
      promptForBuilder: null
    }
  };

  res.json(testResponses[testIntent] || testResponses.call_contact);
});

app.listen(PORT, () => {
  console.log(`🚀 Bangla Voice Assistant Backend running on port ${PORT}`);
  console.log(`📡 Health check: http://localhost:${PORT}/health`);
});
