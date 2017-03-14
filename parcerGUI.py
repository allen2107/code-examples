# -*- coding: utf-8 -*-
from Tkinter import *
from scrapy.crawler import CrawlerProcess
from scrapy.utils.project import get_project_settings

def beenClicked():
    process = CrawlerProcess(get_project_settings())
	#получение ссылки из текстового поля
    target_url = textField.get()
    process.crawl('darsispider', target_url)
    process.start()
    return
	
#вставить текст в поле
def paste():
    clipboardData = app.selection_get(selection="CLIPBOARD")
    textField.delete(0, 'end')
    textField.insert(0, clipboardData)

app = Tk()
app.title('Мой парсер')
app.geometry('450x200')
app.resizable(True, False) #размер программы можно изменить по горизонтали

app_frame=Frame(app,bd=20)
start_text = StringVar(None)
textField = Entry(app_frame, width=50, textvariable=start_text)
textField.pack()

paste_button = Button(app_frame, text="Вставить текст", width=20,command=paste)
paste_button.pack(side='top',padx=15,pady=15)

button1 = Button(app_frame, text="Запуск!", width=20,command=beenClicked)
button1.pack(side='bottom',padx=15,pady=15)

app_frame.pack()

app.mainloop()
