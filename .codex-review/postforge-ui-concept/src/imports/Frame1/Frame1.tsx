import svgPaths from "./svg-bj3ylttj0l";

function Text() {
  return (
    <div className="h-[20px] relative shrink-0 w-[9.344px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex items-start relative size-full">
        <p className="font-['Noto_Sans_KR:Bold',sans-serif] leading-[20px] not-italic relative shrink-0 text-[#1a1613] text-[14px] whitespace-nowrap">P</p>
      </div>
    </div>
  );
}

function RootLayout1() {
  return (
    <div className="bg-white relative rounded-[4px] shrink-0 size-[28px]" data-name="RootLayout">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex items-center justify-center px-[9.328px] relative size-full">
        <Text />
      </div>
    </div>
  );
}

function RootLayout2() {
  return (
    <div className="flex-[1_0_0] h-[28px] min-h-px min-w-px relative" data-name="RootLayout">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Serif_KR:SemiBold',sans-serif] font-semibold leading-[28px] left-0 text-[18px] text-white top-0 whitespace-nowrap">PostForge</p>
      </div>
    </div>
  );
}

function Link() {
  return (
    <div className="h-[28px] relative shrink-0 w-[124.438px]" data-name="Link">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[8px] items-center relative size-full">
        <RootLayout1 />
        <RootLayout2 />
      </div>
    </div>
  );
}

function Input() {
  return (
    <div className="absolute bg-[#2a2723] h-[36px] left-[8px] rounded-[6px] top-0 w-[646.484px]" data-name="Input">
      <div className="content-stretch flex items-center overflow-clip pl-[40px] pr-[12px] py-[4px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[normal] relative shrink-0 text-[#8a8680] text-[14px] whitespace-nowrap">종목명, 키워드 검색...</p>
      </div>
      <div aria-hidden="true" className="absolute border border-[#3a3733] border-solid inset-0 pointer-events-none rounded-[6px]" />
    </div>
  );
}

function Icon() {
  return (
    <div className="absolute left-[12px] size-[16px] top-[10px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16 16">
        <g id="Icon">
          <path d={svgPaths.p107a080} id="Vector" stroke="var(--stroke-0, #8A8680)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d="M14 14L11.1333 11.1333" id="Vector_2" stroke="var(--stroke-0, #8A8680)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
        </g>
      </svg>
    </div>
  );
}

function Form() {
  return (
    <div className="absolute h-[36px] left-0 right-0 top-0" data-name="Form">
      <Input />
      <Icon />
    </div>
  );
}

function Container() {
  return (
    <div className="h-[36px] relative shrink-0 w-[646.484px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Form />
      </div>
    </div>
  );
}

function Icon1() {
  return (
    <div className="absolute left-[10px] size-[16px] top-[8px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16 16">
        <g id="Icon">
          <path d={svgPaths.p1bb15080} id="Vector" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
        </g>
      </svg>
    </div>
  );
}

function Link1() {
  return (
    <div className="absolute h-[32px] left-0 rounded-[6px] top-0 w-[90.719px]" data-name="Link">
      <Icon1 />
      <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[20px] left-[38px] text-[#c5c0b8] text-[14px] top-[4px] whitespace-nowrap">AI 대화</p>
    </div>
  );
}

function Icon2() {
  return (
    <div className="absolute left-[10px] size-[16px] top-[8px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16 16">
        <g clipPath="url(#clip0_20_1509)" id="Icon">
          <path d={svgPaths.p874e300} id="Vector" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d="M13.3333 2V4.66667" id="Vector_2" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d="M14.6667 3.33333H12" id="Vector_3" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d="M2.66667 11.3333V12.6667" id="Vector_4" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d="M3.33333 12H2" id="Vector_5" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
        </g>
        <defs>
          <clipPath id="clip0_20_1509">
            <rect fill="white" height="16" width="16" />
          </clipPath>
        </defs>
      </svg>
    </div>
  );
}

function Link2() {
  return (
    <div className="absolute h-[32px] left-[94.72px] rounded-[6px] top-0 w-[90.719px]" data-name="Link">
      <Icon2 />
      <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[20px] left-[38px] text-[#c5c0b8] text-[14px] top-[4px] whitespace-nowrap">AI 생성</p>
    </div>
  );
}

function Icon3() {
  return (
    <div className="absolute left-[10px] size-[16px] top-[8px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16 16">
        <g id="Icon">
          <path d={svgPaths.p38f39800} id="Vector" stroke="var(--stroke-0, white)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d={svgPaths.p85cdd00} id="Vector_2" stroke="var(--stroke-0, white)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
        </g>
      </svg>
    </div>
  );
}

function Link3() {
  return (
    <div className="absolute bg-[#b8956a] h-[32px] left-[193.44px] rounded-[6px] top-0 w-[86.641px]" data-name="Link">
      <Icon3 />
      <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[20px] left-[38px] text-[14px] text-white top-[4px] whitespace-nowrap">글쓰기</p>
    </div>
  );
}

function Container2() {
  return <div className="absolute bg-[#3a3733] h-[20px] left-[292.08px] top-[6px] w-px" data-name="Container" />;
}

function Icon4() {
  return (
    <div className="relative shrink-0 size-[16px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16 16">
        <g id="Icon">
          <path d={svgPaths.p399eca00} id="Vector" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d={svgPaths.pc93b400} id="Vector_2" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
        </g>
      </svg>
    </div>
  );
}

function Link4() {
  return (
    <div className="absolute content-stretch flex h-[32px] items-center justify-center left-[305.08px] px-[10px] rounded-[6px] top-0 w-[36px]" data-name="Link">
      <Icon4 />
    </div>
  );
}

function Icon5() {
  return (
    <div className="relative shrink-0 size-[16px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16 16">
        <g id="Icon">
          <path d={svgPaths.p12257fa0} id="Vector" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d={svgPaths.p2c1f680} id="Vector_2" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
          <path d="M14 8H6" id="Vector_3" stroke="var(--stroke-0, #C5C0B8)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.33333" />
        </g>
      </svg>
    </div>
  );
}

function Button() {
  return (
    <div className="absolute content-stretch flex h-[32px] items-center justify-center left-[345.08px] px-[10px] rounded-[6px] top-0 w-[36px]" data-name="Button">
      <Icon5 />
    </div>
  );
}

function Container1() {
  return (
    <div className="h-[32px] relative shrink-0 w-[381.078px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Link1 />
        <Link2 />
        <Link3 />
        <Container2 />
        <Link4 />
        <Button />
      </div>
    </div>
  );
}

function RootLayout() {
  return (
    <div className="bg-[#1a1613] h-[56px] relative shrink-0 w-full" data-name="RootLayout">
      <div className="flex flex-row items-center size-full">
        <div className="content-stretch flex items-center justify-between px-[483.5px] relative size-full">
          <Link />
          <Container />
          <Container1 />
        </div>
      </div>
    </div>
  );
}

function Text1() {
  return <div className="absolute bg-[#1a1613] left-[11.63px] rounded-[30023630px] size-[5.369px] top-[8.95px]" data-name="Text" />;
}

function Container4() {
  return (
    <div className="h-[23.264px] relative shrink-0 w-[178.438px]" data-name="Container">
      <div aria-hidden="true" className="absolute border-[#1a1613] border-[0.895px] border-solid inset-0 pointer-events-none" />
      <Text1 />
      <p className="-translate-x-1/2 absolute font-['Noto_Sans_KR:Bold',sans-serif] leading-[14.316px] left-[95.87px] not-italic text-[#1a1613] text-[10.737px] text-center top-[4.47px] tracking-[1.0737px] uppercase whitespace-nowrap">Market Insights</p>
    </div>
  );
}

function Heading() {
  return (
    <div className="h-[134.217px] relative shrink-0 w-[801.72px]" data-name="Heading 1">
      <p className="-translate-x-1/2 absolute font-['Noto_Serif_KR:Medium',sans-serif] font-medium leading-[67.108px] left-[401.33px] text-[#1a1613] text-[53.686px] text-center top-0 whitespace-nowrap">AI 기반 한국 주식 분석 커뮤니티</p>
    </div>
  );
}

function Paragraph() {
  return (
    <div className="h-[58.16px] relative shrink-0 w-[601.29px]" data-name="Paragraph">
      <p className="-translate-x-1/2 absolute font-['Noto_Sans_KR:Light',sans-serif] font-light leading-[29.08px] left-[300.64px] text-[#6b6861] text-[17.895px] text-center top-[-2.68px] w-[601.289px]">실시간 공시 분석과 AI 생성 리포트로 데이터 기반 투자 인사이트를 명확히 제공합니다.</p>
    </div>
  );
}

function Icon6() {
  return (
    <div className="absolute left-[14.32px] size-[14.316px] top-[10.74px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 14.3165 14.3165">
        <g id="Icon">
          <path d={svgPaths.p55eea00} id="Vector" stroke="var(--stroke-0, #FAF9F6)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.19303" />
          <path d="M11.9312 1.78957V4.17564" id="Vector_2" stroke="var(--stroke-0, #FAF9F6)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.19303" />
          <path d="M13.1233 2.98261H10.7373" id="Vector_3" stroke="var(--stroke-0, #FAF9F6)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.19303" />
          <path d="M2.38684 10.1408V11.3339" id="Vector_4" stroke="var(--stroke-0, #FAF9F6)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.19303" />
          <path d="M2.98268 10.7374H1.78965" id="Vector_5" stroke="var(--stroke-0, #FAF9F6)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.19303" />
        </g>
      </svg>
    </div>
  );
}

function Button1() {
  return (
    <div className="bg-[#1a1613] h-[35.791px] relative shrink-0 w-[145.094px]" data-name="Button">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon6 />
        <p className="-translate-x-1/2 absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[17.895px] left-[87.34px] text-[#faf9f6] text-[12.527px] text-center top-[7.16px] whitespace-nowrap">AI 분석 생성하기</p>
      </div>
    </div>
  );
}

function Button2() {
  return (
    <div className="bg-[#faf9f6] h-[35.791px] relative shrink-0 w-[158.278px]" data-name="Button">
      <div aria-hidden="true" className="absolute border-[#1a1613] border-[0.895px] border-solid inset-0 pointer-events-none" />
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex items-center justify-center px-[29.528px] py-[0.895px] relative size-full">
        <p className="font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[17.895px] relative shrink-0 text-[#1a1613] text-[12.527px] text-center whitespace-nowrap">최신 분석 보기</p>
      </div>
    </div>
  );
}

function Container5() {
  return (
    <div className="content-stretch flex gap-[14.316px] h-[35.791px] items-center justify-center pl-[242.009px] pr-[242.023px] relative shrink-0 w-[801.72px]" data-name="Container">
      <Button1 />
      <Button2 />
    </div>
  );
}

function Container3() {
  return (
    <div className="content-stretch flex flex-[1_0_0] flex-col gap-[8.948px] items-center justify-center min-h-px min-w-px relative w-full" data-name="Container">
      <Container4 />
      <Heading />
      <Paragraph />
      <Container5 />
    </div>
  );
}

function Frame2() {
  return (
    <div className="bg-white content-stretch flex flex-col h-[333px] items-center justify-center relative shrink-0 w-[1216px]">
      <Container3 />
    </div>
  );
}

function Frame1() {
  return (
    <div className="content-stretch flex flex-col h-[398px] items-center justify-center py-[8.948px] relative shrink-0 w-full">
      <Frame2 />
    </div>
  );
}

function Icon7() {
  return (
    <div className="absolute left-[8.68px] size-[17.353px] top-[5.96px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 17.3533 17.3533">
        <g id="Icon">
          <path d={svgPaths.p1c519c80} id="Vector" stroke="var(--stroke-0, #1A1613)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
          <path d={svgPaths.p10428e00} id="Vector_2" stroke="var(--stroke-0, #1A1613)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
        </g>
      </svg>
    </div>
  );
}

function Tab() {
  return (
    <div className="absolute bg-white border-[1.085px] border-[rgba(0,0,0,0)] border-solid h-[27.738px] left-[3.58px] rounded-[13.015px] top-[3.58px] w-[112.742px]" data-name="Tab">
      <Icon7 />
      <p className="-translate-x-1/2 absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[21.692px] left-[64.71px] text-[#1a1613] text-[15.184px] text-center top-[1.6px] whitespace-nowrap">전체 보기</p>
    </div>
  );
}

function Icon8() {
  return (
    <div className="absolute left-[8.68px] size-[17.353px] top-[5.96px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 17.3533 17.3533">
        <g id="Icon">
          <path d={svgPaths.p38113300} id="Vector" stroke="var(--stroke-0, #1A1613)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
          <path d="M14.461 2.16917V5.06138" id="Vector_2" stroke="var(--stroke-0, #1A1613)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
          <path d="M15.9072 3.61527H13.015" id="Vector_3" stroke="var(--stroke-0, #1A1613)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
          <path d="M2.89218 12.2919V13.738" id="Vector_4" stroke="var(--stroke-0, #1A1613)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
          <path d="M3.61505 13.015H2.16894" id="Vector_5" stroke="var(--stroke-0, #1A1613)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
        </g>
      </svg>
    </div>
  );
}

function Tab1() {
  return (
    <div className="absolute border-[1.085px] border-[rgba(0,0,0,0)] border-solid h-[31.317px] left-[116.32px] rounded-[13.015px] top-[3.58px] w-[90.373px]" data-name="Tab">
      <Icon8 />
      <p className="-translate-x-1/2 absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[21.692px] left-[57.71px] text-[#1a1613] text-[15.184px] text-center top-[1.63px] whitespace-nowrap">AI 분석</p>
    </div>
  );
}

function Icon9() {
  return (
    <div className="absolute left-[8.67px] size-[17.353px] top-[5.96px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 17.3533 17.3533">
        <g id="Icon">
          <path d={svgPaths.p26577568} id="Vector" stroke="var(--stroke-0, #4A7C59)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
          <path d={svgPaths.p34257640} id="Vector_2" stroke="var(--stroke-0, #4A7C59)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
        </g>
      </svg>
    </div>
  );
}

function Tab2() {
  return (
    <div className="absolute border-[1.085px] border-[rgba(0,0,0,0)] border-solid h-[31.317px] left-[206.69px] right-[123.48px] rounded-[13.015px] top-[3.58px]" data-name="Tab">
      <Icon9 />
      <p className="-translate-x-1/2 absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[21.692px] left-[72.3px] text-[#1a1613] text-[15.184px] text-center top-[1.63px] whitespace-nowrap">상승 신호</p>
    </div>
  );
}

function Icon10() {
  return (
    <div className="relative size-[17.353px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 17.3533 17.3533">
        <g id="Icon">
          <path d={svgPaths.pc2c4660} id="Vector" stroke="var(--stroke-0, #C76B5E)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
          <path d={svgPaths.p17ea97c0} id="Vector_2" stroke="var(--stroke-0, #C76B5E)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4461" />
        </g>
      </svg>
    </div>
  );
}

function Tab3() {
  return (
    <div className="absolute border-[1.085px] border-[rgba(0,0,0,0)] border-solid h-[31.317px] left-[326.59px] right-[3.58px] rounded-[13.015px] top-[3.58px]" data-name="Tab">
      <div className="absolute flex items-center justify-center left-[8.68px] size-[17.353px] top-[5.96px]">
        <div className="flex-none rotate-180">
          <Icon10 />
        </div>
      </div>
      <p className="-translate-x-1/2 absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[21.692px] left-[72.3px] text-[#1a1613] text-[15.184px] text-center top-[1.63px] whitespace-nowrap">하락 신호</p>
    </div>
  );
}

function TabList() {
  return (
    <div className="absolute bg-[#e8e6e1] h-[39.37px] left-0 rounded-[13.015px] top-[16.11px] w-[442.914px]" data-name="Tab List">
      <Tab />
      <Tab1 />
      <Tab2 />
      <Tab3 />
    </div>
  );
}

function Icon11() {
  return (
    <div className="h-[22.605px] overflow-clip relative shrink-0 w-full" data-name="Icon">
      <div className="absolute inset-[29.17%_8.33%_29.17%_8.34%]" data-name="Vector">
        <div className="absolute inset-[-10%_-5%]">
          <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 20.7212 11.3025">
            <path d={svgPaths.pb339e40} id="Vector" stroke="var(--stroke-0, #4A7C59)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.88374" />
          </svg>
        </div>
      </div>
      <div className="absolute inset-[29.17%_8.33%_45.83%_66.67%]" data-name="Vector">
        <div className="absolute inset-[-16.67%]">
          <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 7.53497 7.53497">
            <path d={svgPaths.p39360180} id="Vector" stroke="var(--stroke-0, #4A7C59)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.88374" />
          </svg>
        </div>
      </div>
    </div>
  );
}

function Container9() {
  return (
    <div className="absolute content-stretch flex flex-col items-start left-0 size-[22.605px] top-[4.52px]" data-name="Container">
      <Icon11 />
    </div>
  );
}

function Heading1() {
  return (
    <div className="absolute content-stretch flex h-[30.517px] items-start left-0 overflow-clip top-0 w-[981.052px]" data-name="Heading 3">
      <p className="flex-[1_0_0] font-['Noto_Serif_KR:Medium',sans-serif] font-medium leading-[30.517px] min-h-px min-w-px relative text-[#1a1613] text-[20.344px]">삼성전자 실적 발표 분석: HBM3E 수요 급증과 전망</p>
    </div>
  );
}

function Paragraph1() {
  return (
    <div className="absolute h-[45.21px] left-0 overflow-clip top-[39.56px] w-[981.052px]" data-name="Paragraph">
      <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[22.605px] left-0 text-[#6b6861] text-[15.823px] top-[-2.26px] w-[981.05px]">삼성전자 최신 실적을 심층 분석했습니다. HBM3E 메모리 수요가 예상보다 빠르게 증가하며 AI 서버 시장 확대로 수혜가 기대됩니다.</p>
    </div>
  );
}

function Text2() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[24.865px] left-0 rounded-[6.781px] top-0 w-[70.27px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.172px] py-[3.39px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.084px] relative shrink-0 text-[#1a1613] text-[13.563px] whitespace-nowrap">삼성전자</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.13px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.781px]" />
    </div>
  );
}

function Text3() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[24.865px] left-[79.31px] rounded-[6.781px] top-0 w-[57.784px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.172px] py-[3.39px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.084px] relative shrink-0 text-[#1a1613] text-[13.563px] whitespace-nowrap">반도체</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.13px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.781px]" />
    </div>
  );
}

function Text4() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[24.865px] left-[146.14px] rounded-[6.781px] top-0 w-[50.155px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.172px] py-[3.39px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.084px] relative shrink-0 text-[#1a1613] text-[13.563px] whitespace-nowrap">HBM</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.13px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.781px]" />
    </div>
  );
}

function Text5() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[24.865px] left-[205.33px] rounded-[6.781px] top-0 w-[70.27px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.172px] py-[3.39px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.084px] relative shrink-0 text-[#1a1613] text-[13.563px] whitespace-nowrap">실적 분석</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.13px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.781px]" />
    </div>
  );
}

function Container11() {
  return (
    <div className="absolute h-[24.865px] left-0 top-[98.33px] w-[981.052px]" data-name="Container">
      <Text2 />
      <Text3 />
      <Text4 />
      <Text5 />
    </div>
  );
}

function Text7() {
  return <div className="absolute bg-[#b8956a] left-0 rounded-[37924584px] size-[6.781px] top-[5.65px]" data-name="Text" />;
}

function Text6() {
  return (
    <div className="flex-[1_0_0] h-[18.084px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Text7 />
        <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[18.084px] left-[11.3px] text-[#b8956a] text-[13.563px] top-[-2.26px] whitespace-nowrap">AI 분석가</p>
      </div>
    </div>
  );
}

function Text8() {
  return (
    <div className="h-[18.084px] relative shrink-0 w-[48.76px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.084px] left-0 text-[#6b6861] text-[13.563px] top-[-2.26px] whitespace-nowrap">2시간 전</p>
      </div>
    </div>
  );
}

function Container13() {
  return (
    <div className="h-[20.344px] relative shrink-0 w-[127.488px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.563px] items-center relative size-full">
        <Text6 />
        <Text8 />
      </div>
    </div>
  );
}

function Icon12() {
  return (
    <div className="absolute left-0 size-[15.823px] top-[1.13px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 15.8234 15.8234">
        <g clipPath="url(#clip0_20_1444)" id="Icon">
          <path d={svgPaths.pd99b100} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.31862" />
          <path d={svgPaths.p2ae8aa00} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.31862" />
        </g>
        <defs>
          <clipPath id="clip0_20_1444">
            <rect fill="white" height="15.8234" width="15.8234" />
          </clipPath>
        </defs>
      </svg>
    </div>
  );
}

function Text9() {
  return (
    <div className="flex-[1_0_0] h-[18.084px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon12 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.084px] left-[20.34px] text-[#6b6861] text-[13.563px] top-[-2.26px] whitespace-nowrap">12,847</p>
      </div>
    </div>
  );
}

function Icon13() {
  return (
    <div className="absolute left-0 size-[15.823px] top-[1.13px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 15.8234 15.8234">
        <g id="Icon">
          <path d={svgPaths.p3f445a00} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.31862" />
        </g>
      </svg>
    </div>
  );
}

function Text10() {
  return (
    <div className="h-[18.084px] relative shrink-0 w-[42.932px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon13 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.084px] left-[20.34px] text-[#6b6861] text-[13.563px] top-[-2.26px] whitespace-nowrap">142</p>
      </div>
    </div>
  );
}

function Icon14() {
  return (
    <div className="absolute left-0 size-[15.823px] top-[1.13px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 15.8234 15.8234">
        <g clipPath="url(#clip0_20_1479)" id="Icon">
          <path d="M4.61523 6.59313V14.5049" id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.31862" />
          <path d={svgPaths.p19036600} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.31862" />
        </g>
        <defs>
          <clipPath id="clip0_20_1479">
            <rect fill="white" height="15.8234" width="15.8234" />
          </clipPath>
        </defs>
      </svg>
    </div>
  );
}

function Text11() {
  return (
    <div className="h-[18.084px] relative shrink-0 w-[42.932px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon14 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.084px] left-[20.34px] text-[#6b6861] text-[13.563px] top-[-2.26px] whitespace-nowrap">326</p>
      </div>
    </div>
  );
}

function Container14() {
  return (
    <div className="h-[18.084px] relative shrink-0 w-[174.764px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.563px] items-center relative size-full">
        <Text9 />
        <Text10 />
        <Text11 />
      </div>
    </div>
  );
}

function Container12() {
  return (
    <div className="absolute content-stretch flex h-[20.344px] items-center justify-between left-0 top-[136.76px] w-[981.052px]" data-name="Container">
      <Container13 />
      <Container14 />
    </div>
  );
}

function Container10() {
  return (
    <div className="absolute h-[157.104px] left-[36.17px] top-0 w-[981.052px]" data-name="Container">
      <Heading1 />
      <Paragraph1 />
      <Container11 />
      <Container12 />
    </div>
  );
}

function Link5() {
  return (
    <div className="h-[157.104px] relative shrink-0 w-[1017.22px]" data-name="Link">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Container9 />
        <Container10 />
      </div>
    </div>
  );
}

function Container8() {
  return (
    <div className="bg-white h-[227.18px] relative rounded-[13.563px] shrink-0 w-full" data-name="Container">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-[1.13px] border-solid inset-0 pointer-events-none rounded-[13.563px]" />
      <div className="content-stretch flex flex-col items-start p-[28.256px] relative size-full">
        <Link5 />
      </div>
    </div>
  );
}

function Icon15() {
  return (
    <div className="h-[22.887px] overflow-clip relative shrink-0 w-full" data-name="Icon">
      <div className="absolute inset-[29.17%_8.33%]" data-name="Vector">
        <div className="absolute inset-[-10%_-5%]">
          <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 20.9802 11.4437">
            <path d={svgPaths.p160fa880} id="Vector" stroke="var(--stroke-0, #4A7C59)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.90728" />
          </svg>
        </div>
      </div>
      <div className="absolute inset-[29.17%_8.33%_45.83%_66.67%]" data-name="Vector">
        <div className="absolute inset-[-16.67%]">
          <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 7.62916 7.62916">
            <path d={svgPaths.pc027600} id="Vector" stroke="var(--stroke-0, #4A7C59)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.90728" />
          </svg>
        </div>
      </div>
    </div>
  );
}

function Container16() {
  return (
    <div className="absolute content-stretch flex flex-col items-start left-0 size-[22.887px] top-[4.58px]" data-name="Container">
      <Icon15 />
    </div>
  );
}

function Heading2() {
  return (
    <div className="absolute content-stretch flex h-[30.898px] items-start left-0 overflow-clip top-0 w-[993.316px]" data-name="Heading 3">
      <p className="flex-[1_0_0] font-['Noto_Serif_KR:Medium',sans-serif] font-medium leading-[30.898px] min-h-px min-w-px relative text-[#1a1613] text-[20.599px]">SK하이닉스 주가 급등, 목표가 상향 배경 분석</p>
    </div>
  );
}

function Paragraph2() {
  return (
    <div className="absolute content-stretch flex h-[22.887px] items-start left-0 overflow-clip top-[40.05px] w-[993.316px]" data-name="Paragraph">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[22.887px] min-h-px min-w-px relative text-[#6b6861] text-[16.021px]">주요 증권사들이 목표가를 상향 조정했습니다. AI 반도체 수요 확대와 HBM 시장 점유율 상승이 주요 원인입니다.</p>
    </div>
  );
}

function Text12() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-0 rounded-[6.866px] top-0 w-[88.206px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">SK하이닉스</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text13() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[97.36px] rounded-[6.866px] top-0 w-[58.506px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">반도체</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text14() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[165.02px] rounded-[6.866px] top-0 w-[58.506px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">목표가</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Container18() {
  return (
    <div className="absolute h-[25.176px] left-0 top-[76.67px] w-[993.316px]" data-name="Container">
      <Text12 />
      <Text13 />
      <Text14 />
    </div>
  );
}

function Text15() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[18.31px] left-0 text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">투자의 달인</p>
      </div>
    </div>
  );
}

function Text16() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[49.369px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-0 text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">5시간 전</p>
      </div>
    </div>
  );
}

function Container20() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[126.275px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text15 />
        <Text16 />
      </div>
    </div>
  );
}

function Icon16() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p2df65900} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.p3b868680} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text17() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon16 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">8,234</p>
      </div>
    </div>
  );
}

function Icon17() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p3c463200} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text18() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[35.851px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon17 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">89</p>
      </div>
    </div>
  );
}

function Icon18() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d="M4.67255 6.6755V14.6861" id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.pf278e00} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text19() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[43.468px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon18 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">201</p>
      </div>
    </div>
  );
}

function Container21() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[161.714px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text17 />
        <Text18 />
        <Text19 />
      </div>
    </div>
  );
}

function Container19() {
  return (
    <div className="absolute content-stretch flex h-[18.31px] items-center justify-between left-0 top-[115.58px] w-[993.316px]" data-name="Container">
      <Container20 />
      <Container21 />
    </div>
  );
}

function Container17() {
  return (
    <div className="absolute h-[133.892px] left-[36.62px] top-0 w-[993.316px]" data-name="Container">
      <Heading2 />
      <Paragraph2 />
      <Container18 />
      <Container19 />
    </div>
  );
}

function Link6() {
  return (
    <div className="h-[133.892px] relative shrink-0 w-[1029.935px]" data-name="Link">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Container16 />
        <Container17 />
      </div>
    </div>
  );
}

function Container15() {
  return (
    <div className="bg-white h-[204.843px] relative rounded-[14.311px] shrink-0 w-full" data-name="Container">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-[1.193px] border-solid inset-0 pointer-events-none rounded-[14.311px]" />
      <div className="content-stretch flex flex-col items-start p-[29.814px] relative size-full">
        <Link6 />
      </div>
    </div>
  );
}

function Heading3() {
  return (
    <div className="absolute content-stretch flex h-[30.898px] items-start left-0 overflow-clip top-0 w-[1016.203px]" data-name="Heading 3">
      <p className="flex-[1_0_0] font-['Noto_Serif_KR:Medium',sans-serif] font-medium leading-[30.898px] min-h-px min-w-px relative text-[#1a1613] text-[20.599px]">네이버 클라우드 사업 확장, AI 서비스 투자 강화</p>
    </div>
  );
}

function Paragraph3() {
  return (
    <div className="absolute content-stretch flex h-[22.887px] items-start left-0 overflow-clip top-[40.05px] w-[1016.203px]" data-name="Paragraph">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[22.887px] min-h-px min-w-px relative text-[#6b6861] text-[16.021px]">네이버가 클라우드 사업을 확장하며 AI 서비스 투자를 크게 늘린다고 발표했습니다. 하이퍼클로바X 기반 B2B 서비스 확대가 기대됩니다.</p>
    </div>
  );
}

function Text20() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-0 rounded-[6.866px] top-0 w-[63.334px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">NAVER</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text21() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[72.49px] rounded-[6.866px] top-0 w-[71.148px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">클라우드</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text22() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[152.79px] rounded-[6.866px] top-0 w-[32.972px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">AI</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text23() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[194.92px] rounded-[6.866px] top-0 w-[96.414px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">하이퍼클로바</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Container24() {
  return (
    <div className="absolute h-[25.176px] left-0 top-[76.67px] w-[1016.203px]" data-name="Container">
      <Text20 />
      <Text21 />
      <Text22 />
      <Text23 />
    </div>
  );
}

function Text25() {
  return <div className="absolute bg-[#b8956a] left-0 rounded-[38398640px] size-[6.866px] top-[5.72px]" data-name="Text" />;
}

function Text24() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Text25 />
        <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[18.31px] left-[11.44px] text-[#b8956a] text-[13.732px] top-[-2.29px] whitespace-nowrap">AI 분석가</p>
      </div>
    </div>
  );
}

function Text26() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[36.745px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-0 text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">1일 전</p>
      </div>
    </div>
  );
}

function Container26() {
  return (
    <div className="h-[20.599px] relative shrink-0 w-[116.458px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text24 />
        <Text26 />
      </div>
    </div>
  );
}

function Icon19() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p2cacf680} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.p37dae500} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text27() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon19 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">6,521</p>
      </div>
    </div>
  );
}

function Icon20() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p26cbcc00} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text28() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[35.851px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon20 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">67</p>
      </div>
    </div>
  );
}

function Icon21() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d="M4.67255 6.67534V14.686" id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.p19c5c800} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text29() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[43.468px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon21 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">154</p>
      </div>
    </div>
  );
}

function Container27() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[161.714px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text27 />
        <Text28 />
        <Text29 />
      </div>
    </div>
  );
}

function Container25() {
  return (
    <div className="absolute content-stretch flex h-[20.599px] items-center justify-between left-0 top-[115.58px] w-[1016.203px]" data-name="Container">
      <Container26 />
      <Container27 />
    </div>
  );
}

function Container23() {
  return (
    <div className="flex-[888_0_0] h-[136.18px] min-h-px min-w-px relative" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Heading3 />
        <Paragraph3 />
        <Container24 />
        <Container25 />
      </div>
    </div>
  );
}

function Link7() {
  return (
    <div className="h-[136.18px] relative shrink-0 w-[1029.935px]" data-name="Link">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex items-start pl-[13.732px] relative size-full">
        <Container23 />
      </div>
    </div>
  );
}

function Container22() {
  return (
    <div className="bg-white h-[207.132px] relative rounded-[14.311px] shrink-0 w-full" data-name="Container">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-[1.193px] border-solid inset-0 pointer-events-none rounded-[14.311px]" />
      <div className="content-stretch flex flex-col items-start p-[29.814px] relative size-full">
        <Link7 />
      </div>
    </div>
  );
}

function Heading4() {
  return (
    <div className="absolute content-stretch flex h-[30.898px] items-start left-0 overflow-clip top-0 w-[1016.203px]" data-name="Heading 3">
      <p className="flex-[1_0_0] font-['Noto_Serif_KR:Medium',sans-serif] font-medium leading-[30.898px] min-h-px min-w-px relative text-[#1a1613] text-[20.599px]">카카오 사업구조 개편 및 투자 포인트</p>
    </div>
  );
}

function Paragraph4() {
  return (
    <div className="absolute content-stretch flex h-[22.887px] items-start left-0 overflow-clip top-[40.05px] w-[1016.203px]" data-name="Paragraph">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[22.887px] min-h-px min-w-px relative text-[#6b6861] text-[16.021px]">카카오가 사업구조 개편을 본격화하며 핵심 사업 집중과 비핵심 사업 정리로 수익성 개선이 기대됩니다.</p>
    </div>
  );
}

function Text30() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-0 rounded-[6.866px] top-0 w-[58.506px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">카카오</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text31() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[67.66px] rounded-[6.866px] top-0 w-[71.148px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">구조조정</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text32() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[147.96px] rounded-[6.866px] top-0 w-[71.148px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">투자 전략</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Container30() {
  return (
    <div className="absolute h-[25.176px] left-0 top-[76.67px] w-[1016.203px]" data-name="Container">
      <Text30 />
      <Text31 />
      <Text32 />
    </div>
  );
}

function Text33() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[18.31px] left-0 text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">마켓인사이트</p>
      </div>
    </div>
  );
}

function Text34() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[36.745px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-0 text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">1일 전</p>
      </div>
    </div>
  );
}

function Container32() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[126.292px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text33 />
        <Text34 />
      </div>
    </div>
  );
}

function Icon22() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p30064a00} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.p1e9bba00} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text35() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon22 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">5,432</p>
      </div>
    </div>
  );
}

function Icon23() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p28f7cb00} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text36() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[35.851px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon23 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">45</p>
      </div>
    </div>
  );
}

function Icon24() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d="M4.67255 6.67562V14.6862" id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.pc761200} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text37() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[43.468px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon24 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">112</p>
      </div>
    </div>
  );
}

function Container33() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[161.714px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text35 />
        <Text36 />
        <Text37 />
      </div>
    </div>
  );
}

function Container31() {
  return (
    <div className="absolute content-stretch flex h-[18.31px] items-center justify-between left-0 top-[115.58px] w-[1016.203px]" data-name="Container">
      <Container32 />
      <Container33 />
    </div>
  );
}

function Container29() {
  return (
    <div className="flex-[888_0_0] h-[133.892px] min-h-px min-w-px relative" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Heading4 />
        <Paragraph4 />
        <Container30 />
        <Container31 />
      </div>
    </div>
  );
}

function Link8() {
  return (
    <div className="h-[133.892px] relative shrink-0 w-[1029.935px]" data-name="Link">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex items-start pl-[13.732px] relative size-full">
        <Container29 />
      </div>
    </div>
  );
}

function Container28() {
  return (
    <div className="bg-white h-[204.843px] relative rounded-[14.311px] shrink-0 w-full" data-name="Container">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-[1.193px] border-solid inset-0 pointer-events-none rounded-[14.311px]" />
      <div className="content-stretch flex flex-col items-start p-[29.814px] relative size-full">
        <Link8 />
      </div>
    </div>
  );
}

function Icon25() {
  return (
    <div className="h-[22.887px] overflow-clip relative shrink-0 w-full" data-name="Icon">
      <div className="absolute inset-[29.17%_8.33%]" data-name="Vector">
        <div className="absolute inset-[-10%_-5%]">
          <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 20.9802 11.4437">
            <path d={svgPaths.p37930000} id="Vector" stroke="var(--stroke-0, #C76B5E)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.90728" />
          </svg>
        </div>
      </div>
      <div className="absolute inset-[45.83%_8.33%_29.17%_66.67%]" data-name="Vector">
        <div className="absolute inset-[-16.67%]">
          <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 7.62916 7.62916">
            <path d={svgPaths.p27dd5d80} id="Vector" stroke="var(--stroke-0, #C76B5E)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.90728" />
          </svg>
        </div>
      </div>
    </div>
  );
}

function Container35() {
  return (
    <div className="absolute content-stretch flex flex-col items-start left-0 size-[22.887px] top-[4.58px]" data-name="Container">
      <Icon25 />
    </div>
  );
}

function Heading5() {
  return (
    <div className="absolute content-stretch flex h-[30.898px] items-start left-0 overflow-clip top-0 w-[993.316px]" data-name="Heading 3">
      <p className="flex-[1_0_0] font-['Noto_Serif_KR:Medium',sans-serif] font-medium leading-[30.898px] min-h-px min-w-px relative text-[#1a1613] text-[20.599px]">현대차 전기차 판매 부진, 재고 증가 우려</p>
    </div>
  );
}

function Paragraph5() {
  return (
    <div className="absolute content-stretch flex h-[22.887px] items-start left-0 overflow-clip top-[40.05px] w-[993.316px]" data-name="Paragraph">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[22.887px] min-h-px min-w-px relative text-[#6b6861] text-[16.021px]">현대차 전기차 판매가 예상보다 저조해 재고 증가 우려가 커지고 있습니다. 보조금 축소와 경쟁 심화가 원인입니다.</p>
    </div>
  );
}

function Text38() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-0 rounded-[6.866px] top-0 w-[58.506px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">현대차</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text39() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[67.66px] rounded-[6.866px] top-0 w-[58.506px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">전기차</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text40() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[135.32px] rounded-[6.866px] top-0 w-[45.882px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">실적</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Container37() {
  return (
    <div className="absolute h-[25.176px] left-0 top-[76.67px] w-[993.316px]" data-name="Container">
      <Text38 />
      <Text39 />
      <Text40 />
    </div>
  );
}

function Text42() {
  return <div className="absolute bg-[#b8956a] left-0 rounded-[38398640px] size-[6.866px] top-[5.72px]" data-name="Text" />;
}

function Text41() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Text42 />
        <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[18.31px] left-[11.44px] text-[#b8956a] text-[13.732px] top-[-2.29px] whitespace-nowrap">AI 분석가</p>
      </div>
    </div>
  );
}

function Text43() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[36.745px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-0 text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">2일 전</p>
      </div>
    </div>
  );
}

function Container39() {
  return (
    <div className="h-[20.599px] relative shrink-0 w-[116.458px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text41 />
        <Text43 />
      </div>
    </div>
  );
}

function Icon26() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p60e7400} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.p219af100} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text44() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon26 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">9,871</p>
      </div>
    </div>
  );
}

function Icon27() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p3b50b400} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text45() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[43.468px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon27 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">134</p>
      </div>
    </div>
  );
}

function Icon28() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d="M4.6727 6.67577V14.6864" id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.p625e500} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text46() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[35.851px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon28 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">78</p>
      </div>
    </div>
  );
}

function Container40() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[161.714px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text44 />
        <Text45 />
        <Text46 />
      </div>
    </div>
  );
}

function Container38() {
  return (
    <div className="absolute content-stretch flex h-[20.599px] items-center justify-between left-0 top-[115.58px] w-[993.316px]" data-name="Container">
      <Container39 />
      <Container40 />
    </div>
  );
}

function Container36() {
  return (
    <div className="absolute h-[136.18px] left-[36.62px] top-0 w-[993.316px]" data-name="Container">
      <Heading5 />
      <Paragraph5 />
      <Container37 />
      <Container38 />
    </div>
  );
}

function Link9() {
  return (
    <div className="h-[136.18px] relative shrink-0 w-[1029.935px]" data-name="Link">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Container35 />
        <Container36 />
      </div>
    </div>
  );
}

function Container34() {
  return (
    <div className="bg-white h-[207.132px] relative rounded-[14.311px] shrink-0 w-full" data-name="Container">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-[1.193px] border-solid inset-0 pointer-events-none rounded-[14.311px]" />
      <div className="content-stretch flex flex-col items-start p-[29.814px] relative size-full">
        <Link9 />
      </div>
    </div>
  );
}

function Icon29() {
  return (
    <div className="h-[22.887px] overflow-clip relative shrink-0 w-full" data-name="Icon">
      <div className="absolute inset-[29.17%_8.33%]" data-name="Vector">
        <div className="absolute inset-[-10%_-5%]">
          <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 20.9802 11.4437">
            <path d={svgPaths.p160fa880} id="Vector" stroke="var(--stroke-0, #4A7C59)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.90728" />
          </svg>
        </div>
      </div>
      <div className="absolute inset-[29.17%_8.33%_45.83%_66.67%]" data-name="Vector">
        <div className="absolute inset-[-16.67%]">
          <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 7.62916 7.62916">
            <path d={svgPaths.pc027600} id="Vector" stroke="var(--stroke-0, #4A7C59)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.90728" />
          </svg>
        </div>
      </div>
    </div>
  );
}

function Container42() {
  return (
    <div className="absolute content-stretch flex flex-col items-start left-0 size-[22.887px] top-[4.58px]" data-name="Container">
      <Icon29 />
    </div>
  );
}

function Heading6() {
  return (
    <div className="absolute content-stretch flex h-[30.898px] items-start left-0 overflow-clip top-0 w-[993.316px]" data-name="Heading 3">
      <p className="flex-[1_0_0] font-['Noto_Serif_KR:Medium',sans-serif] font-medium leading-[30.898px] min-h-px min-w-px relative text-[#1a1613] text-[20.599px]">LG에너지솔루션 배터리 수주 확대, 북미 시장 성장</p>
    </div>
  );
}

function Paragraph6() {
  return (
    <div className="absolute content-stretch flex h-[22.887px] items-start left-0 overflow-clip top-[40.05px] w-[993.316px]" data-name="Paragraph">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[22.887px] min-h-px min-w-px relative text-[#6b6861] text-[16.021px]">LG에너지솔루션이 북미에서 대규모 배터리 수주에 성공했습니다. IRA 보조금 혜택과 함께 실적 개선이 기대됩니다.</p>
    </div>
  );
}

function Text47() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-0 rounded-[6.866px] top-0 w-[112.9px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">LG에너지솔루션</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text48() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[122.05px] rounded-[6.866px] top-0 w-[58.506px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">배터리</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Text49() {
  return (
    <div className="absolute bg-[#f5f3ef] h-[25.176px] left-[189.72px] rounded-[6.866px] top-0 w-[71.148px]" data-name="Text">
      <div className="content-stretch flex items-center justify-center overflow-clip px-[10.299px] py-[3.433px] relative rounded-[inherit] size-full">
        <p className="font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] relative shrink-0 text-[#1a1613] text-[13.732px] whitespace-nowrap">북미 시장</p>
      </div>
      <div aria-hidden="true" className="absolute border-[1.144px] border-[rgba(0,0,0,0)] border-solid inset-0 pointer-events-none rounded-[6.866px]" />
    </div>
  );
}

function Container44() {
  return (
    <div className="absolute h-[25.176px] left-0 top-[76.67px] w-[993.316px]" data-name="Container">
      <Text47 />
      <Text48 />
      <Text49 />
    </div>
  );
}

function Text50() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[18.31px] left-0 text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">배터리워치</p>
      </div>
    </div>
  );
}

function Text51() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[36.745px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-0 text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">2일 전</p>
      </div>
    </div>
  );
}

function Container46() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[113.651px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text50 />
        <Text51 />
      </div>
    </div>
  );
}

function Icon30() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p2d75a000} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.p1d18300} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text52() {
  return (
    <div className="flex-[1_0_0] h-[18.31px] min-h-px min-w-px relative" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon30 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">4,321</p>
      </div>
    </div>
  );
}

function Icon31() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d={svgPaths.p15656b00} id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text53() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[35.851px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon31 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">56</p>
      </div>
    </div>
  );
}

function Icon32() {
  return (
    <div className="absolute left-0 size-[16.021px] top-[1.14px]" data-name="Icon">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 16.0212 16.0212">
        <g id="Icon">
          <path d="M4.67255 6.6755V14.6861" id="Vector" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
          <path d={svgPaths.p42cdb00} id="Vector_2" stroke="var(--stroke-0, #6B6861)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.3351" />
        </g>
      </svg>
    </div>
  );
}

function Text54() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[43.468px]" data-name="Text">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Icon32 />
        <p className="absolute font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[18.31px] left-[20.6px] text-[#6b6861] text-[13.732px] top-[-2.29px] whitespace-nowrap">143</p>
      </div>
    </div>
  );
}

function Container47() {
  return (
    <div className="h-[18.31px] relative shrink-0 w-[161.714px]" data-name="Container">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid content-stretch flex gap-[13.732px] items-center relative size-full">
        <Text52 />
        <Text53 />
        <Text54 />
      </div>
    </div>
  );
}

function Container45() {
  return (
    <div className="absolute content-stretch flex h-[18.31px] items-center justify-between left-0 top-[115.58px] w-[993.316px]" data-name="Container">
      <Container46 />
      <Container47 />
    </div>
  );
}

function Container43() {
  return (
    <div className="absolute h-[133.892px] left-[36.62px] top-0 w-[993.316px]" data-name="Container">
      <Heading6 />
      <Paragraph6 />
      <Container44 />
      <Container45 />
    </div>
  );
}

function Link10() {
  return (
    <div className="h-[133.892px] relative shrink-0 w-[1029.935px]" data-name="Link">
      <div className="bg-clip-padding border-0 border-[transparent] border-solid relative size-full">
        <Container42 />
        <Container43 />
      </div>
    </div>
  );
}

function Container41() {
  return (
    <div className="bg-white h-[204.843px] relative rounded-[14.311px] shrink-0 w-full" data-name="Container">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-[1.193px] border-solid inset-0 pointer-events-none rounded-[14.311px]" />
      <div className="content-stretch flex flex-col items-start p-[29.814px] relative size-full">
        <Link10 />
      </div>
    </div>
  );
}

function Container7() {
  return (
    <div className="content-stretch flex flex-col gap-[17.001px] items-start relative shrink-0 w-full" data-name="Container">
      <Container8 />
      <Container15 />
      <Container22 />
      <Container28 />
      <Container34 />
      <Container41 />
    </div>
  );
}

function Button3() {
  return (
    <div className="bg-[#faf9f6] h-[43.383px] relative rounded-[6.507px] shrink-0 w-full" data-name="Button">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-[1.085px] border-solid inset-0 pointer-events-none rounded-[6.507px]" />
      <p className="-translate-x-1/2 absolute font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[21.692px] left-[calc(50%-0.32px)] text-[#1a1613] text-[15.184px] text-center top-[8.68px] whitespace-nowrap">더 보기</p>
    </div>
  );
}

function Container48() {
  return (
    <div className="h-[43.383px] relative shrink-0 w-full" data-name="Container">
      <div className="content-stretch flex flex-col items-start px-[464.978px] relative size-full">
        <Button3 />
      </div>
    </div>
  );
}

function TabPanel() {
  return (
    <div className="absolute content-stretch flex flex-col gap-[34.706px] h-[1506.422px] items-start left-0 right-[-0.15px] top-[73.75px]" data-name="Tab Panel">
      <Container7 />
      <Container48 />
    </div>
  );
}

function Container6() {
  return (
    <div className="h-[1509px] relative shrink-0 w-[1088px]" data-name="Container">
      <TabList />
      <TabPanel />
    </div>
  );
}

function HomePage() {
  return (
    <div className="absolute bg-[#faf9f6] content-stretch flex flex-col h-[1915px] items-center left-[483px] right-[484px] top-0" data-name="HomePage">
      <Frame1 />
      <Container6 />
    </div>
  );
}

function Heading7() {
  return (
    <div className="content-stretch flex h-[27px] items-start relative shrink-0 w-full" data-name="Heading 3">
      <p className="flex-[1_0_0] font-['Noto_Serif_KR:Medium',sans-serif] font-medium leading-[27px] min-h-px min-w-px relative text-[#1a1613] text-[18px]">PostForge</p>
    </div>
  );
}

function Paragraph7() {
  return (
    <div className="content-stretch flex h-[20px] items-start relative shrink-0 w-full" data-name="Paragraph">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[20px] min-h-px min-w-px relative text-[#6b6861] text-[14px]">AI 기반 한국 주식 분석 커뮤니티</p>
    </div>
  );
}

function Container51() {
  return (
    <div className="absolute content-stretch flex flex-col gap-[12px] h-[112px] items-start left-0 top-0 w-[384px]" data-name="Container">
      <Heading7 />
      <Paragraph7 />
    </div>
  );
}

function Heading8() {
  return (
    <div className="content-stretch flex h-[24px] items-start relative shrink-0 w-full" data-name="Heading 4">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[24px] min-h-px min-w-px relative text-[#1a1613] text-[16px]">서비스</p>
    </div>
  );
}

function ListItem() {
  return (
    <div className="content-stretch flex h-[20px] items-start relative shrink-0 w-full" data-name="List Item">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[20px] min-h-px min-w-px relative text-[#6b6861] text-[14px]">AI 대화</p>
    </div>
  );
}

function ListItem1() {
  return (
    <div className="content-stretch flex h-[20px] items-start relative shrink-0 w-full" data-name="List Item">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[20px] min-h-px min-w-px relative text-[#6b6861] text-[14px]">AI 분석 생성</p>
    </div>
  );
}

function ListItem2() {
  return (
    <div className="content-stretch flex h-[20px] items-start relative shrink-0 w-full" data-name="List Item">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[20px] min-h-px min-w-px relative text-[#6b6861] text-[14px]">분석 게시판</p>
    </div>
  );
}

function List() {
  return (
    <div className="content-stretch flex flex-col gap-[8px] h-[76px] items-start relative shrink-0 w-full" data-name="List">
      <ListItem />
      <ListItem1 />
      <ListItem2 />
    </div>
  );
}

function Container52() {
  return (
    <div className="absolute content-stretch flex flex-col gap-[12px] h-[112px] items-start left-[416px] top-0 w-[384px]" data-name="Container">
      <Heading8 />
      <List />
    </div>
  );
}

function Heading9() {
  return (
    <div className="content-stretch flex h-[24px] items-start relative shrink-0 w-full" data-name="Heading 4">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Medium',sans-serif] font-medium leading-[24px] min-h-px min-w-px relative text-[#1a1613] text-[16px]">정보</p>
    </div>
  );
}

function ListItem3() {
  return (
    <div className="content-stretch flex h-[20px] items-start relative shrink-0 w-full" data-name="List Item">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[20px] min-h-px min-w-px relative text-[#6b6861] text-[14px]">이용 약관</p>
    </div>
  );
}

function ListItem4() {
  return (
    <div className="content-stretch flex h-[20px] items-start relative shrink-0 w-full" data-name="List Item">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[20px] min-h-px min-w-px relative text-[#6b6861] text-[14px]">개인정보 처리방침</p>
    </div>
  );
}

function ListItem5() {
  return (
    <div className="content-stretch flex h-[20px] items-start relative shrink-0 w-full" data-name="List Item">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[20px] min-h-px min-w-px relative text-[#6b6861] text-[14px]">문의하기</p>
    </div>
  );
}

function List1() {
  return (
    <div className="content-stretch flex flex-col gap-[8px] h-[76px] items-start relative shrink-0 w-full" data-name="List">
      <ListItem3 />
      <ListItem4 />
      <ListItem5 />
    </div>
  );
}

function Container53() {
  return (
    <div className="absolute content-stretch flex flex-col gap-[12px] h-[112px] items-start left-[832px] top-0 w-[384px]" data-name="Container">
      <Heading9 />
      <List1 />
    </div>
  );
}

function Container50() {
  return (
    <div className="h-[112px] relative shrink-0 w-full" data-name="Container">
      <Container51 />
      <Container52 />
      <Container53 />
    </div>
  );
}

function Paragraph8() {
  return (
    <div className="content-stretch flex h-[20px] items-start relative shrink-0 w-full" data-name="Paragraph">
      <p className="flex-[1_0_0] font-['Noto_Sans_KR:Regular',sans-serif] font-normal leading-[20px] min-h-px min-w-px relative text-[#6b6861] text-[14px] text-center">© 2026 PostForge. All rights reserved.</p>
    </div>
  );
}

function Container54() {
  return (
    <div className="content-stretch flex flex-col h-[53px] items-start pt-[33px] relative shrink-0 w-full" data-name="Container">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-solid border-t inset-0 pointer-events-none" />
      <Paragraph8 />
    </div>
  );
}

function Container49() {
  return (
    <div className="h-[261px] relative shrink-0 w-full" data-name="Container">
      <div className="content-stretch flex flex-col gap-[32px] items-start pt-[32px] px-[32px] relative size-full">
        <Container50 />
        <Container54 />
      </div>
    </div>
  );
}

function Footer() {
  return (
    <div className="absolute bg-white content-stretch flex flex-col h-[262px] items-start left-[483px] pt-px right-[484px] top-[1914px]" data-name="Footer">
      <div aria-hidden="true" className="absolute border-[#d4d1cb] border-solid border-t inset-0 pointer-events-none" />
      <Container49 />
    </div>
  );
}

function RootLayout3() {
  return (
    <div className="h-[2176px] relative shrink-0 w-full" data-name="RootLayout">
      <HomePage />
      <Footer />
    </div>
  );
}

export default function Frame() {
  return (
    <div className="bg-[rgba(60,60,67,0.29)] content-stretch flex flex-col items-start relative size-full">
      <RootLayout />
      <RootLayout3 />
    </div>
  );
}