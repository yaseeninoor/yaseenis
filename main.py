from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from langchain_community.document_loaders import DirectoryLoader
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_community.vectorstores import FAISS
from langchain.chains import RetrievalQA
import os

app = FastAPI()

# Allow all origins for the web frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load documents and create vector store
loader = DirectoryLoader('./pdfs', glob="**/*.pdf")
documents = loader.load()
embeddings = OpenAIEmbeddings()
vectorstore = FAISS.from_documents(documents, embeddings)
qa_chain = RetrievalQA.from_chain_type(llm=ChatOpenAI(), chain_type="stuff", retriever=vectorstore.as_retriever())

@app.get("/ask/")
async def ask(question: str):
    try:
        answer = qa_chain.run(question)
        return {"answer": answer}
    except Exception as e:
        return {"error": str(e)}
