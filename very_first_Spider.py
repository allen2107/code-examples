# -*- encoding: utf-8 -*-
import json
import logging
from scrapy.spiders import CrawlSpider, Rule, Spider
from scrapy.linkextractors.sgml import SgmlLinkExtractor
from scrapy.loader.processors import TakeFirst, Identity
from scrapy.loader import ItemLoader
from scrapy.selector import Selector
from very_first_parser.items import parserItem
from scrapy.http import Request, FormRequest

class very_first_Loader(ItemLoader):
    default_output_processor = Identity()

class very_first_spider(Spider):
    name = "very_first_spider"
    allowed_domains = ["site_main_domain"]
    start_urls = ["siteurl"]

	#получение ссылки на целевую страницу из GUI
    def __init__(self, target_url):
        self.target_url = target_url

	#авторизация на сайте
    def parse(self, response):
        formdata = {'email': 'myemail', 'password': 'mypw'}
        yield FormRequest.from_response(response, formnumber=0, formdata=formdata, clickdata={'type': 'submit'}, callback=self.new_url, dont_filter=True)

	#переход по ссылке для сбора данных
    def new_url(self, response):
        return Request(self.target_url, callback=self.parse_item)

    def parse_item(self, response):
        logging.debug ("parse_start_url")
        hxs = Selector(response)
        l = very_first_Loader(parserItem(), hxs)
        numbers = len(hxs.xpath("//table[@class='tblVert']/tr[@class='pfrow']"))
        current_row = 1 #текущая строка таблицы на сайте
		for i in range(1, numbers+1):
			#получение id, имени, артикула, цены
            current_row = current_row + 1
            l.replace_xpath('id', "//table[@class='tblVert']/tr[@class='pfrow'][%d]/td[1]/text()" % i)
            l.replace_xpath('name', "//table[@class='tblVert']/tr[@class='pfrow'][%d]/td[3]/a/text()[1]" % i)
            l.replace_xpath('articul', "//table[@class='tblVert']/tr[@class='pfrow'][%d]/td[3]/a/text()[2]" % i)
            l.replace_xpath('price', "//table[@class='tblVert']/tr[@class='pfrow'][%d]/td[@class='price']/text()" % i)
			#кол-во цветов
            colornbrs = len(hxs.xpath("//table[@class='tblVert']/tr[count(preceding-sibling::tr[@class='pfrow'])=%d]" % i)) - 1
            while colornbrs>0:
                #получение цветов и размеров
                l.replace_xpath('color', "//table[@class='tblVert']/tr[%d]/td[@class='clr']/span/text()" % current_row)
				#кол-во размеров
                sizesnbrs = len(hxs.xpath("//table[@class='tblVert']/tr[%d]/td[@class='clr']/following-sibling::td[1]/table//text()" % current_row))
                for numberofsize in range(1, sizesnbrs+1):
                    l.replace_xpath('sizes', "//table[@class='tblVert']/tr[%d]/td[@class='clr']/following-sibling::td[1]/table/tr[%d]/td/text()" % (current_row, numberofsize))
                    yield l.load_item() #записать в файл строку данных
                colornbrs=colornbrs-1
                current_row=current_row+1
