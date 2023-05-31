
import Adafruit_DHT # 온도 센서용 라이브러리
import RPi.GPIO as GPIO # GPIO 핀 라이브러리
import time
import os
from twilio.rest import Client # 문자 전송 모듈
import requests # HTTP 요청을 위한 라이브러리

# 17번 = 불꽃 감지 (input)
# 27번 = 온도 센서 (input)
# 22번 = 피에조 부저 (output)
fire_pin, temp_pin, buzzer_pin = 17, 27, 22

GPIO.setmode(GPIO.BCM) # GPIO 핀맵 사용
GPIO.setup(fire_pin, GPIO.IN) # 불꽃 감지 센서를 핀의 입력으로 받음.

temp_sensor = Adafruit_DHT.DHT11 # 온도 센서 객체 생성

GPIO.setup(buzzer_pin, GPIO.OUT) # 부저
GPIO.setwarnings(False) # GPIO 경고 무시

# <1> 온도 경계값. 특정 온도로 변경하고 싶으면 이 변수 변경할 것. 테스트 시 25도를 기준으로 함
temp_thr = 25 

pwm = GPIO.PWM(buzzer_pin, 1) # 부저 핀 지정, 주파수 지정.

# 메세지 전송을 위한 SID, 토큰, 클라이언트 객체 생성
# <2> twillio 가입 후, 계정에서 아래의 내용들(sid, token)을 얻어야 함.
account_sid = ''
auth_token = ''
client = Client(account_sid, auth_token) # 

# motion을 사용한 촬영/스트리밍 백그라운드에서 시작
os.system('sudo motion n &')

# oneM2M 기반의 Open Soruce Software IoT server platform인 Mobius 서버를 활용.

# http://203.253.128.177:7575/#!/monitor 주소로 접근한 후
# 두 개의 Target Resource에 각각 http://203.253.128.161:7579와 /Mobius/Fire_sensing를 입력하여 모니터링 가능

# 온도 값 저장을 위한 URL 주소와 헤더 설정
# 서버에서 실제 생성되는게 아니라 포맷을 미리 지정하는 것.
url_temp = 'http://203.253.128.161:7579/Mobius/Fire_sensing/Temp' 
headers_temp =	{
        'Accept':'application/json',
        'X-M2M-RI':'12345',
        'X-M2M-Origin':'Fire_sensing',
        'Content-Type':'application/vnd.onem2m-res+json; ty=4'
}

# 불꽃 감지 값 저장을 위한 URL 주소와 헤더 설정. 위와 유사함.
url_fire = 'http://203.253.128.161:7579/Mobius/Fire_sensing/Fire'
headers_fire =	{
        'Accept':'application/json',
        'X-M2M-RI':'12345',
        'X-M2M-Origin':'Fire_sensing',
        'Content-Type':'application/vnd.onem2m-res+json; ty=4'
}

# 아래의 코드들은 모두 주기적으로 반복
while(1):
    # 불꽃 센서 값 출력 (RPI 모니터링용)
    print('The value of fire : {}'.format(GPIO.input(fire_pin)))
    try:
    # 온도 값 출력
        hum, temp = Adafruit_DHT.read_retry(temp_sensor, temp_pin) # 센서 값(습도, 온도) 읽기
        print('The value of Temp : {}'.format(temp)) # 온도만 출력(RPI 모니터링용)

    # 서버에 업로드 하기 위해 온도 데이터를 갱신하여 저장.
    # 위에 작성된 헤더에서 json 형태로 송신하기로 하였기 때문에 json 포맷으로 송신.
        data_temp =	{
            "m2m:cin": {
               "con": temp # con이라는 키 값에 온도 값 저장
            }
        }

        if (GPIO.input(fire_pin) == 1): # 화재 발생 시
            data_fire =	{
                "m2m:cin": {
                    "con": "화재 발생" # 위와 동일. 화재 발생 문자열을 con 키 값에 저장
            }
        } 
        else: # 정상 상태인 경우
            data_fire =	{
                "m2m:cin": {
                    "con": "정상" # 위와 동일.
             }
        }

    # 앞에서 저장한 각각의 url로 헤더 파일과 함께 데이터를 송신하여 서버에 온도, 불꽃 감지 여부를 저장함.
        requests.post(url_temp, headers=headers_temp, json=data_temp)
        requests.post(url_fire, headers=headers_fire, json=data_fire)

    # 불꽃이 감지되거나 특정 온도 이상일 때
        if (GPIO.input(fire_pin) == False and (temp  >= temp_thr)):
        # 이 구간에 화재 감지 동작.

            print("fire detected.") #(RPI 모니터링용)

            print("Send message") #(RPI 모니터링용)
        # 메세지 객체를 생성하여 등록된 번호로 메시지 전송됨
        # <3> twillio에 가입하여 등록된 가상 번호와 실제 휴대폰 번호를 입력해주어야 함
            message = client.messages.create(
                body='화재가 감지되었습니다!', # 필요시 다른 메시지 입력
                from_='+', # 가상 번호
                to='+82'  # 실제 수신 번호
            )

            pwm.start(50) # 사이렌을 위한 pwm제어

        # for문 두개 -> 사이렌
            for i in range(300, 750):
                pwm.ChangeFrequency(i)
                time.sleep(0.01) # 1.5초간

            for i in range(750, 300, -1):
                pwm.ChangeFrequency(i)
                time.sleep(0.01) # 1.5초간
        
                pwm.stop() # 소리 중단
        
        else:
                print("nothing happened.") # (RPI 모니터링용)

                time.sleep(0.5) # 최소한의 딜레이를 걸어야 라파가 안뻗음.
    except:
        ''''''

